package org.tron.defi.contract_mirror.core;

import org.tron.defi.contract.log.ContractLog;
import org.tron.defi.contract_mirror.dao.KafkaMessage;

public interface Synchronizable {
    boolean isEventAccept();

    boolean isReady();

    void sync();

    void onEvent(KafkaMessage<ContractLog> kafkaMessage,
                 long syncPeriod) throws InterruptedException;
}
