package org.tron.defi.contract_mirror.core;

import lombok.extern.slf4j.Slf4j;
import org.tron.defi.contract.abi.EventPrototype;
import org.tron.defi.contract.log.ContractLog;
import org.tron.defi.contract_mirror.dao.BlockInfo;
import org.tron.defi.contract_mirror.dao.KafkaMessage;
import org.web3j.abi.EventValues;

@Slf4j
public abstract class SynchronizableContract extends Contract implements Synchronizable {
    protected long timestamp0 = 0;
    protected long timestamp1 = 0;
    protected long timestamp2 = 0;
    protected BlockInfo lastBlock;
    protected long lastEventTimestamp;
    protected boolean ready = false;

    public SynchronizableContract(String address) {
        super(address);
    }

    @Override
    public boolean isEventAccept() {
        return timestamp0 > 0 && timestamp1 > timestamp0;
    }

    @Override
    public void onEvent(KafkaMessage<ContractLog> kafkaMessage, long syncPeriod) {
        if (!isEventAccept()) {
            throw new IllegalStateException();
        }
        ContractLog contractLog = kafkaMessage.getMessage();
        if (timestamp0 > 0 &&
            syncPeriod > 0 &&
            contractLog.getTimeStamp() - timestamp0 >= syncPeriod) {
            // force sync period
            timestamp1 = 0;
            throw new IllegalStateException();
        }
        if (contractLog.getTimeStamp() < timestamp0) {
            // not interest
            return;
        } else if (contractLog.getTimeStamp() < timestamp1) {
            throw new IllegalStateException();
        }
        BlockInfo blockInfo = new BlockInfo(contractLog.getBlockNumber(),
                                            contractLog.getBlockHash());
        if (contractLog.isRemoved() ||
            contractLog.getTimeStamp() < lastEventTimestamp ||
            (null != lastBlock &&
             (blockInfo.isEarlier(lastBlock) || blockInfo.isConflict(lastBlock)))) {
            // handle chain switch
            throw new IllegalStateException();
        }
        try {
            EventPrototype prototype = abi.getEvent(contractLog.getRawData().getTopics()[0]);
            log.info("On " + prototype.getRawSignature() + " event");
            EventValues eventValues = abi.decodeEvent(contractLog);
            handleEvent(prototype.getName(), eventValues, contractLog.getTimeStamp());
            log.debug("Processed event " + contractLog);
        } catch (NullPointerException npe) {
            npe.printStackTrace();
            log.warn("Unsupported event: " + contractLog);
        } catch (RuntimeException e) {
            e.printStackTrace();
            log.error("Failed event: " + contractLog);
            throw new IllegalStateException();
        }
        lastBlock = blockInfo;
        lastEventTimestamp = contractLog.getTimeStamp();
    }

    protected abstract void handleEvent(String eventName, EventValues eventValues, long eventTime);

    protected void checkEventTimestamp(long eventTime) {
        if (eventTime >= timestamp0 && eventTime < timestamp2) {
            throw new IllegalStateException();
        }
    }
}
