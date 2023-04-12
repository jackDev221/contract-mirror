package org.tron.defi.contract_mirror.core.consumer;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.tron.defi.contract.log.ContractLog;
import org.tron.defi.contract_mirror.config.ServerConfig;
import org.tron.defi.contract_mirror.core.Contract;
import org.tron.defi.contract_mirror.core.ContractManager;
import org.tron.defi.contract_mirror.core.SynchronizableContract;
import org.tron.defi.contract_mirror.dao.KafkaMessage;
import org.tron.defi.contract_mirror.utils.ShardedThreadPool;

@Slf4j
public class SharedEventConsumer extends Thread implements IEventConsumer {
    private final ServerConfig serverConfig;
    private final ContractManager contractManager;
    private final IEventConsumer fallbackConsumer;
    private ShardedThreadPool eventPool;

    public SharedEventConsumer(ServerConfig serverConfig,
                               ContractManager contractManager,
                               IEventConsumer fallbackConsumer) {
        this.serverConfig = serverConfig;
        this.contractManager = contractManager;
        this.fallbackConsumer = fallbackConsumer;
        if (null != serverConfig.getEventPoolConfig()) {
            eventPool = new ShardedThreadPool(serverConfig.getEventPoolConfig());
        } else {
            log.info("No event pool config, handle event in consume thread");
        }
    }

    @Override
    public void consume(KafkaMessage<ContractLog> message) {
        Contract contract = contractManager.getContract(message.getMessage().getContractAddress());
        if (null == contract ||
            !SynchronizableContract.class.isAssignableFrom(contract.getClass())) {
            return;
        }
        SynchronizableContract contractToSync = (SynchronizableContract) contract;
        if (isFallback(message)) {
            fallbackConsume(contractToSync, message);
        } else if (contractToSync.isEventAccept()) {
            if (null == eventPool) {
                // handle immediately
                handleEvent(contractToSync, message);
            } else {
                ShardedThreadPool.ShardTask task = new ShardEventTask(contractToSync, message);
                eventPool.execute(task);
            }
        } else {
            fallbackConsume(contractToSync, message);
        }
    }

    @Override
    public boolean isFallback(KafkaMessage<ContractLog> message) {
        return null != fallbackConsumer && fallbackConsumer.isFallback(message);
    }

    private void fallbackConsume(SynchronizableContract contract,
                                 KafkaMessage<ContractLog> message) {
        if (null == fallbackConsumer) {
            contract.sync();
            return;
        }
        log.warn("Fallback {}", message.getMessage());
        fallbackConsumer.consume(message);
    }

    private void handleEvent(SynchronizableContract contract, KafkaMessage<ContractLog> message) {
        try {
            contract.onEvent(message, serverConfig.getSyncPeriod());
        } catch (InterruptedException ex) {
            log.warn("{} is not accepting event", contract.getAddress());
            fallbackConsume(contract, message);
        } catch (IllegalStateException e) {
            log.warn("{} need re-sync", contract.getAddress());
            contract.sync();
        } catch (Exception unexpectedError) {
            unexpectedError.printStackTrace();
            log.error(unexpectedError.getMessage());
            contract.sync();
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

    private class ShardEventTask extends ShardedThreadPool.ShardTask {
        private final SynchronizableContract contract;
        private final KafkaMessage<ContractLog> message;

        public ShardEventTask(SynchronizableContract contract, KafkaMessage<ContractLog> message) {
            super(contract.getAddress(), SimpleShardMethod.getInstance());
            this.contract = contract;
            this.message = message;
        }

        @Override
        public void run() {
            handleEvent(contract, message);
        }
    }
}
