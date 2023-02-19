package org.tron.defi.contract_mirror.core;

import org.tron.defi.contract.abi.EventPrototype;
import org.tron.defi.contract.log.ContractLog;
import org.tron.defi.contract_mirror.dao.KafkaMessage;
import org.web3j.abi.EventValues;

public abstract class SynchronizableContract extends Contract implements Synchronizable {
    private int timestamp0;
    private int timestamp1;
    private int timestamp2;
    private int lastBlockNumber;
    private boolean ready;

    public SynchronizableContract(String address) {
        super(address);
    }

    @Override
    public Boolean isReady() {
        return isEventAccept() && ready;
    }

    @Override
    public Boolean isEventAccept() {
        return timestamp2 > timestamp0;
    }

    @Override
    public void onEvent(KafkaMessage<ContractLog> kafkaMessage) {
        EventPrototype prototype = abi.getEvent(kafkaMessage.getMessage()
                                                            .getRawData()
                                                            .getTopics()[0]);
        EventValues eventValues = abi.decodeEvent(kafkaMessage.getMessage());
        handleEvent(prototype.getName(), eventValues);
    }

    protected abstract void handleEvent(String eventName, EventValues eventValues);
}
