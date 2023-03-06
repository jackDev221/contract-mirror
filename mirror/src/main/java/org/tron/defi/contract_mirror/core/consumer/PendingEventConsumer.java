package org.tron.defi.contract_mirror.core.consumer;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.tron.defi.contract.log.ContractLog;
import org.tron.defi.contract_mirror.config.ServerConfig;
import org.tron.defi.contract_mirror.core.Contract;
import org.tron.defi.contract_mirror.core.ContractManager;
import org.tron.defi.contract_mirror.core.SynchronizableContract;
import org.tron.defi.contract_mirror.dao.KafkaMessage;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;

@Slf4j
public class PendingEventConsumer implements IEventConsumer {
    private final ServerConfig serverConfig;
    private final ContractManager contractManager;
    private final ConcurrentHashMap<String, LinkedBlockingQueue<KafkaMessage<ContractLog>>>
        pendingQueue = new ConcurrentHashMap<>();
    private final ThreadPoolTaskExecutor pendingPool = new ThreadPoolTaskExecutor();

    public PendingEventConsumer(ServerConfig serverConfig, ContractManager contractManager) {
        this.serverConfig = serverConfig;
        this.contractManager = contractManager;

        if (null != serverConfig.getPendingPoolConfig() &&
            serverConfig.getPendingPoolConfig().getThreadNum() > 0) {
            pendingPool.setCorePoolSize(serverConfig.getPendingPoolConfig().getThreadNum());
        } else {
            pendingPool.setCorePoolSize(1);
        }
        pendingPool.initialize();
        log.info("Initialized pending pool, corePoolSize {}", pendingPool.getCorePoolSize());
        for (int i = 0; i < pendingPool.getCorePoolSize(); ++i) {
            pendingPool.execute(new ConsumeTask());
        }
    }

    @Override
    public void consume(KafkaMessage<ContractLog> message) {
        Contract contract = contractManager.getContract(message.getMessage().getContractAddress());
        if (null == contract ||
            !SynchronizableContract.class.isAssignableFrom(contract.getClass())) {
            return;
        }
        try {
            LinkedBlockingQueue<KafkaMessage<ContractLog>> poolQueue = pendingQueue.getOrDefault(
                contract.getAddress(),
                null);
            if (null == poolQueue) {
                poolQueue = new LinkedBlockingQueue<>();
                LinkedBlockingQueue<KafkaMessage<ContractLog>> exists = pendingQueue.putIfAbsent(
                    contract.getAddress(),
                    poolQueue);
                poolQueue = null == exists ? poolQueue : exists;
            }
            poolQueue.put(message);
        } catch (Exception e) {
            e.printStackTrace();
            log.error(e.getMessage());
            // lose message, need sync
            ((SynchronizableContract) contract).sync();
        }
    }

    @Override
    public boolean isFallback(KafkaMessage<ContractLog> message) {
        return pendingQueue.containsKey(message.getMessage().getContractAddress());
    }


    private class ConsumeTask implements Runnable {
        @Override
        public void run() {
            // double while loop for try catch optimization
            while (true) {
                try {
                    while (!pendingQueue.isEmpty()) {
                        consume();
                    }
                } catch (NullPointerException e) {
                    // concurrent remove case
                    continue;
                } catch (Exception ex) {
                    ex.printStackTrace();
                    log.error("PendingEventConsumer error {}", ex.getMessage());
                }
                if (pendingQueue.isEmpty()) {
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        // it's fine doing nothing
                        e.printStackTrace();
                    }
                }
            }
        }

        private void consume() {
            for (String address : pendingQueue.keySet()) {
                SynchronizableContract synchronizableContract
                    = (SynchronizableContract) contractManager.getContract(address);
                if (!synchronizableContract.isEventAccept()) {
                    // pool not ready for message
                    continue;
                }
                // take messages
                LinkedBlockingQueue<KafkaMessage<ContractLog>> messages = pendingQueue.put(address,
                                                                                           null);
                if (null == messages) {
                    // taken by other consumer
                    continue;
                }
                log.info("Processing " + messages.size() + " pending messages for " + address);
                // handle messages
                for (KafkaMessage<ContractLog> message : messages) {
                    if (!handleEvent(synchronizableContract, message)) {
                        // need sync again, drop this messages is safe
                        synchronizableContract.sync();
                        break;
                    }
                }
                // clear pool pending state, if queue of pool is empty
                if (null == pendingQueue.get(address)) {
                    messages = pendingQueue.remove(address);
                    // unfortunately remove after new message arrived
                    if (null != messages) {
                        // try put back
                        messages = pendingQueue.put(address, messages);
                        if (null != messages) {
                            // extreme case, messages disordered
                            synchronizableContract.sync();
                        }
                    }
                }
            }
        }

        private boolean handleEvent(SynchronizableContract contract,
                                    KafkaMessage<ContractLog> message) {
            try {
                contract.onEvent(message, serverConfig.getSyncPeriod());
                return true;
            } catch (InterruptedException ex) {
                log.warn("{} is not accepting event", contract.getAddress());
                return false;
            } catch (IllegalStateException e) {
                log.warn("{} need re-sync", contract.getAddress());
                return false;
            }
        }
    }
}
