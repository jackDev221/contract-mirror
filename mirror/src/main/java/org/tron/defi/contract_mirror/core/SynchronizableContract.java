package org.tron.defi.contract_mirror.core;

import lombok.extern.slf4j.Slf4j;
import org.tron.defi.contract.abi.EventPrototype;
import org.tron.defi.contract.log.ContractLog;
import org.tron.defi.contract_mirror.dao.KafkaMessage;
import org.web3j.abi.EventValues;

@Slf4j
public abstract class SynchronizableContract extends Contract implements Synchronizable {
    protected long timestamp0;
    protected long timestamp1;
    protected long timestamp2;
    protected long lastBlockNumber;
    protected long lastEventTimestamp;
    protected boolean ready;

    public SynchronizableContract(String address) {
        super(address);
    }

    @Override
    public Boolean isReady() {
        return isEventAccept() && ready;
    }

    @Override
    public Boolean isEventAccept() {
        return timestamp0 > 0 && timestamp1 > timestamp0;
    }


    @Override
    public void onEvent(KafkaMessage<ContractLog> kafkaMessage) {
        ContractLog contractLog = kafkaMessage.getMessage();
        if (contractLog.getBlockNumber() < lastBlockNumber ||
            contractLog.getTimeStamp() < lastEventTimestamp) {
            // handle chain switch
            throw new IllegalStateException();
        }
        try {
            EventPrototype prototype = abi.getEvent(contractLog.getRawData().getTopics()[0]);
            EventValues eventValues = abi.decodeEvent(contractLog);
            handleEvent(prototype.getName(), eventValues, contractLog.getTimeStamp());
        } catch (RuntimeException e) {
            log.error(e.getMessage());
            throw new IllegalArgumentException();
        }
        lastBlockNumber = contractLog.getBlockNumber();
        lastEventTimestamp = contractLog.getTimeStamp();
    }

    protected abstract void handleEvent(String eventName, EventValues eventValues, long eventTime);

    protected void checkEventTimestamp(long eventTime) {
        if (eventTime >= timestamp0 && eventTime < timestamp2) {
            throw new IllegalStateException();
        }
    }
}
