package org.tron.defi.contract_mirror.core.consumer;

import lombok.extern.slf4j.Slf4j;
import org.tron.defi.contract.log.ContractLog;
import org.tron.defi.contract_mirror.core.Contract;
import org.tron.defi.contract_mirror.core.ContractManager;
import org.tron.defi.contract_mirror.core.SynchronizableContract;
import org.tron.defi.contract_mirror.dao.KafkaMessage;

@Slf4j
public class DiffEventConsumer implements IEventConsumer {
    private final ContractManager contractManager;
    private double totalCount = 0;
    private double diffCount = 0;

    public DiffEventConsumer(ContractManager contractManager) {
        this.contractManager = contractManager;
    }

    @Override
    public void consume(KafkaMessage<ContractLog> message) {
        Contract contract = contractManager.getContract(message.getMessage().getContractAddress());
        if (null == contract ||
            !SynchronizableContract.class.isAssignableFrom(contract.getClass())) {
            return;
        }
        totalCount += 1;
        SynchronizableContract contractToSync = (SynchronizableContract) contract;
        if (contractToSync.diff(message)) {
            log.error("DIFF AFTER {}, LastEventTimestamp {}",
                      message.getMessage(),
                      contractToSync.getLastEventTimestamp());
            diffCount += 1;
        }
        if (totalCount % 100 == 0) {
            log.info("DIFF RATIO {}%", 100 * diffCount / totalCount);
        }
    }

    @Override
    public boolean isFallback(KafkaMessage<ContractLog> message) {
        return false;
    }
}
