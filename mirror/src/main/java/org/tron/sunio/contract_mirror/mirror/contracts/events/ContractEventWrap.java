package org.tron.sunio.contract_mirror.mirror.contracts.events;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.tron.sunio.contract_mirror.event_decode.LogDecode;
import org.tron.sunio.contract_mirror.event_decode.logdata.ContractEventLog;
import org.tron.sunio.contract_mirror.event_decode.logdata.ContractLog;
import org.tron.sunio.contract_mirror.mirror.consts.ContractMirrorConst;
import org.tron.sunio.contract_mirror.mirror.enums.EventLogType;

@Slf4j
@Data
public class ContractEventWrap implements IContractEventWrap {
    private EventLogType eventLogType;
    private ContractLog eventWrap;
    private ContractEventLog contractEventLog;

    public ContractEventWrap(ContractLog eventWrap) {
        this.eventLogType = EventLogType.CONTRACT_LOG;
        this.eventWrap = eventWrap;
    }

    public ContractEventWrap(ContractEventLog contractEventLog) {
        this.eventLogType = EventLogType.CONTRACT_EVENT_LOG;
        this.contractEventLog = contractEventLog;
    }

    @Override
    public long getTimeStamp() {
        if (eventLogType == EventLogType.CONTRACT_EVENT_LOG) {
            return contractEventLog.getTimeStamp();
        } else {
            return eventWrap.getTimeStamp();
        }
    }

    @Override
    public String[] getTopics() {
        if (eventLogType == EventLogType.CONTRACT_EVENT_LOG) {
            return contractEventLog.getRawData().getTopics();
        } else {
            return eventWrap.getTopicList();
        }
    }

    @Override
    public String getData() {
        if (eventLogType == EventLogType.CONTRACT_EVENT_LOG) {
            return contractEventLog.getRawData().getData();
        } else {
            return eventWrap.getData();
        }
    }

    @Override
    public String getUniqueId() {
        if (eventLogType == EventLogType.CONTRACT_EVENT_LOG) {
            return contractEventLog.getUniqueId();
        } else {
            return eventWrap.getUniqueId();
        }
    }

    @Override
    public String getBlockHash() {
        if (eventLogType == EventLogType.CONTRACT_EVENT_LOG) {
            return contractEventLog.getBlockHash();
        } else {
            return eventWrap.getBlockHash();
        }
    }

    @Override
    public long getBlockNumber() {
        if (eventLogType == EventLogType.CONTRACT_EVENT_LOG) {
            return contractEventLog.getBlockNumber();
        } else {
            return eventWrap.getBlockNumber();
        }
    }

    @Override
    public String getContractAddress() {
        if (eventLogType == EventLogType.CONTRACT_EVENT_LOG) {
            return contractEventLog.getContractAddress();
        } else {
            return eventWrap.getContractAddress();
        }
    }

    @Override
    public String updateAndToJson(String[] topicList, String data) {
        if (eventLogType == EventLogType.CONTRACT_EVENT_LOG) {
            return contractEventLog.updateAndToJson(topicList, data);
        } else {
            return eventWrap.updateAndToJson(topicList, data);
        }
    }

    public static ContractEventWrap getInstance(String topic, String value) {
        if (topic.equals(ContractMirrorConst.KAFKA_TOPIC_CONTRACT_LOG)) {
            ContractLog contractLog = LogDecode.decodeContractLog(value);
            return new ContractEventWrap(contractLog);
        }

        if (topic.equals(ContractMirrorConst.KAFKA_TOPIC_CONTRACT_EVENT_LOG)) {
            ContractEventLog contractEventLog = LogDecode.decodeContractEventLog(value);
            return new ContractEventWrap(contractEventLog);
        }
        log.warn("fail to catch topic:{}", topic);
        return null;
    }
}
