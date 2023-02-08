package org.tron.sunio.contract_mirror.mirror.utils;

import org.tron.sunio.contract_mirror.event_decode.logdata.ContractLog;
import org.tron.sunio.contract_mirror.mirror.contracts.events.ContractEventWrap;
import org.tron.sunio.contract_mirror.mirror.contracts.events.IContractEventWrap;

public class EventLogUtils {

    public static IContractEventWrap generateContractEvent(String uniqueID, String[] topics, String data) {
        return generateContractEvent(uniqueID, topics, data, System.currentTimeMillis());
    }

    public static IContractEventWrap generateContractEvent(String uniqueID, String[] topics, String data, long timeStamp) {
        ContractLog contractLog = new ContractLog();
        contractLog.setUniqueId(uniqueID);
        contractLog.setTopicList(topics);
        contractLog.setData(data);
        contractLog.setTimeStamp(timeStamp);
        return new ContractEventWrap(contractLog);
    }
}
