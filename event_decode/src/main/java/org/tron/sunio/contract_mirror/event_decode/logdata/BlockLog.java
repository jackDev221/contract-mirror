package org.tron.sunio.contract_mirror.event_decode.logdata;

import lombok.Data;

@Data
public class BlockLog {
    private long timeStamp;
    private String triggerName;
    private long blockNumber;
    private String blockHash;
    private long transactionSize;
    private long latestSolidifiedBlockNumber;
    private String[] transactionList;
}
