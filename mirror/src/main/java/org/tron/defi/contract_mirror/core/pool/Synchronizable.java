package org.tron.defi.contract_mirror.core.pool;

import org.tron.defi.contract_mirror.dao.ContractLog;
import org.tron.defi.contract_mirror.dao.KafkaMessage;

public interface Synchronizable {
    Boolean isReady();

    Boolean isEventAccept();

    void sync();

    void onEvent(KafkaMessage<ContractLog> kafkaMessage);
}
