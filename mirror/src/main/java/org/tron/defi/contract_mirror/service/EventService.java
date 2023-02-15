package org.tron.defi.contract_mirror.service;

import com.alibaba.fastjson.JSON;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;
import org.tron.defi.contract_mirror.config.KafkaConfig;
import org.tron.defi.contract_mirror.config.ServerConfig;
import org.tron.defi.contract_mirror.core.Contract;
import org.tron.defi.contract_mirror.core.ContractManager;
import org.tron.defi.contract_mirror.core.pool.Pool;
import org.tron.defi.contract_mirror.dao.ContractLog;
import org.tron.defi.contract_mirror.dao.KafkaMessage;
import org.tron.defi.contract_mirror.utils.ShardedThreadPool;
import org.tron.defi.contract_mirror.utils.kafka.FallbackRebalanceListener;

import javax.annotation.PostConstruct;
import java.time.Duration;
import java.util.Collections;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
@Service
public class EventService {
    private final ConcurrentHashMap<String, LinkedBlockingQueue<KafkaMessage<ContractLog>>>
        pendingQueue = new ConcurrentHashMap<>();
    private EventListenThread listenThread;
    @Autowired
    private ServerConfig serverConfig;
    @Autowired
    private KafkaConfig kafkaConfig;
    @Autowired
    private ContractManager contractManager;
    private ShardedThreadPool eventPool;
    private ThreadPoolTaskExecutor pendingPool;
    private KafkaConsumer<Long, String> consumer;

    @PostConstruct
    public void init() {
        if (null != serverConfig.getEventPoolConfig()) {
            eventPool = new ShardedThreadPool(serverConfig.getEventPoolConfig());
        }
        pendingPool = new ThreadPoolTaskExecutor();
        if (null != serverConfig.getPendingPoolConfig()) {
            pendingPool.setCorePoolSize(serverConfig.getPendingPoolConfig().getThreadNum());
        } else {
            pendingPool.setCorePoolSize(1);
        }
        pendingPool.initialize();
        for (int i = 0; i < pendingPool.getCorePoolSize(); ++i) {
            pendingPool.execute(new PendingEventConsumer());
        }
        if (null != kafkaConfig.getConsumerConfig()) {
            consumer = new KafkaConsumer<>(kafkaConfig.getConsumerConfig());
            String topic = kafkaConfig.getConsumerTopics().get(0);
            // initialize kafka start timestamp to now - maxLag
            FallbackRebalanceListener fallbackRebalanceListener = new FallbackRebalanceListener(
                consumer,
                topic,
                kafkaConfig.getMaxLag());
            consumer.subscribe(Collections.singleton(topic), fallbackRebalanceListener);
        }
    }

    public void listen() {
        listenThread = new EventListenThread();
        listenThread.start();
    }

    public void stop() {
        listenThread.cancel();
    }

    private boolean handleEvent(Pool pool, KafkaMessage<ContractLog> message) {
        try {
            pool.onEvent(message);
            return true;
        } catch (IllegalStateException e) {
            log.warn(pool.getAddress() + " is synchronizing.");
            return false;
        }
    }

    private void emitEventTask(Pool pool, KafkaMessage<ContractLog> message) {
        if (null == eventPool) {
            if (!handleEvent(pool, message)) {
                emitPendingTask(pool, message);
            }
        } else {
            ShardedThreadPool.ShardTask task = new EventShardTask(pool, message);
            eventPool.execute(task);
        }
    }

    private void emitPendingTask(Pool pool, KafkaMessage<ContractLog> message) {
        try {
            LinkedBlockingQueue<KafkaMessage<ContractLog>> poolQueue = pendingQueue.getOrDefault(
                pool.getAddress(),
                null);
            if (null == poolQueue) {
                poolQueue = new LinkedBlockingQueue<>();
                LinkedBlockingQueue<KafkaMessage<ContractLog>> exists = pendingQueue.putIfAbsent(
                    pool.getAddress(),
                    poolQueue);
                poolQueue = null == exists ? poolQueue : exists;
            }
            poolQueue.put(message);
        } catch (Exception ex) {
            log.error(ex.getMessage());
            // lose message, need sync
            pool.sync();
        }
    }

    private static class SimpleShardMethod implements ShardedThreadPool.ShardMethod {
        @Getter
        private static final SimpleShardMethod instance = new SimpleShardMethod();

        @Override
        public int shard(String key, int bucketNum) {
            return key.hashCode() % bucketNum;
        }
    }

    private class PendingEventConsumer implements Runnable {
        @Override
        public void run() {
            // double while loop for try catch optimization
            while (true) {
                try {
                    while (!pendingQueue.isEmpty()) {
                        for (String poolAddress : pendingQueue.keySet()) {
                            Pool pool = (Pool) contractManager.getContract(poolAddress);
                            if (!pool.isEventAccept()) {
                                // pool not ready for message
                                continue;
                            }
                            // take messages
                            LinkedBlockingQueue<KafkaMessage<ContractLog>> messages
                                = pendingQueue.put(poolAddress, null);
                            if (null == messages) {
                                // taken by other consumer
                                continue;
                            }
                            // handle messages
                            for (KafkaMessage<ContractLog> message : messages) {
                                if (!handleEvent(pool, message)) {
                                    // need sync again, drop this messages is safe
                                    pool.sync();
                                    break;
                                }
                            }
                            // clear pool pending state, if queue of pool is empty
                            if (null == pendingQueue.get(poolAddress)) {
                                messages = pendingQueue.remove(poolAddress);
                                // unfortunately remove after new message arrived
                                if (null != messages) {
                                    // try put back
                                    messages = pendingQueue.put(poolAddress, messages);
                                    if (null != messages) {
                                        // really bad fortune, messages disordered
                                        pool.sync();
                                    }
                                }
                            }
                        }
                    }
                } catch (NullPointerException e) {
                    // concurrent remove case
                    continue;
                } catch (Exception ex) {
                    log.error(ex.getMessage());
                }
                if (pendingQueue.isEmpty()) {
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        // it's fine doing nothing
                        log.info(e.getMessage());
                    }
                }
            }
        }
    }

    private class EventListenThread extends Thread {
        private final AtomicBoolean listen = new AtomicBoolean(true);

        public void cancel() {
            listen.set(false);
        }

        public void run() {
            while (listen.get()) {
                try {
                    while (listen.get()) {
                        ConsumerRecords<Long, String> consumerRecords
                            = consumer.poll(Duration.ofMillis(kafkaConfig.getMaxLag()));
                        if (0 == consumerRecords.count()) {
                            continue;
                        }
                        while (consumerRecords.iterator().hasNext()) {
                            KafkaMessage<ContractLog> message
                                = new KafkaMessage<>(consumerRecords.iterator().next());
                            String address = message.getMessage().getContractAddress();
                            Contract contract = contractManager.getContract(address);
                            if (null == contract || !(contract instanceof Pool)) {
                                continue;
                            }
                            Pool pool = (Pool) contract;
                            if (!pendingQueue.containsKey(pool.getAddress()) &&
                                pool.isEventAccept()) {
                                emitEventTask(pool, message);
                            } else {
                                emitPendingTask(pool, message);
                            }
                        }
                    }
                } catch (Exception e) {
                    log.error(e.getMessage());
                }
            }
        }
    }

    private class EventShardTask extends ShardedThreadPool.ShardTask {
        private final Pool pool;
        private final KafkaMessage<ContractLog> message;

        public EventShardTask(Pool pool, KafkaMessage<ContractLog> message) {
            super(pool.getAddress(), SimpleShardMethod.getInstance());
            this.pool = pool;
            this.message = message;
        }

        @Override
        public void run() {
            if (!handleEvent(pool, message)) {
                log.warn("Drop event earlier than pool synchronization, " +
                         JSON.toJSONString(message.getMessage()));
            }
        }
    }
}
