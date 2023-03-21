package org.tron.sunio.contract_mirror.event_decode.logdata;

import lombok.Data;

@Data
public class TransactionLog {
    private long timeStamp;
    private String triggerName;
    private String transactionId;
    private String blockHash;
    private long blockNumber;
    private long energyUsage;
    private long energyFee;
    private long originEnergyUsage;
    private long energyUsageTotal;
    private long netUsage;
    private long netFee;
    private String result;
    private String contractAddress;
    private String contractType;
    private long feeLimit;
    private long contractCallValue;
    private String contractResult;
    private String fromAddress;
    private String toAddress;
    private String assetName;
    private long assetAmount;
    private long latestSolidifiedBlockNumber;
    private String[] internalTransactionList;
    private String data;
    private int transactionIndex;
    private long cumulativeEnergyUsed;
    private long preCumulativeLogCount;
    private String[] logList;
    private long energyUnitPrice;
}
