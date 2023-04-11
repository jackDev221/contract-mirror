package org.tron.defi.contract_mirror.core.consumer;

import org.tron.defi.contract.log.ContractLog;
import org.tron.defi.contract_mirror.dao.KafkaMessage;

public interface IEventConsumer {
    void consume(KafkaMessage<ContractLog> message);

    boolean isFallback(KafkaMessage<ContractLog> message);
}
