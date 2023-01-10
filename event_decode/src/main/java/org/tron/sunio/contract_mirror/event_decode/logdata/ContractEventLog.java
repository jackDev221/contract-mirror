package org.tron.sunio.contract_mirror.event_decode.logdata;

import lombok.Data;

import java.util.List;

@Data
public class ContractEventLog {
    private long timeStamp;
    private String triggerName;
    private String uniqueId;
    private String transactionId;
    private String contractAddress;
    private String callerAddress;
    private String originAddress;
    private String creatorAddress;
    private long blockNumber;
    private String blockHash;
    private boolean removed;
    private long latestSolidifiedBlockNumber;
    private RawData rawData;
    private List<String> topicList;
    private String data;
}
