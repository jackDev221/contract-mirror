package org.tron.defi.contract_mirror.dao;

import lombok.Data;

@Data
public class ContractLog {
    /**
     * unique id of this trigger. $tx_id + "_" + $index
     */
    private String uniqueId;

    /**
     * id of the transaction which produce this event.
     */
    private String transactionId;

    /**
     * address of the contract triggered by the callerAddress.
     */
    private String contractAddress;

    /**
     * caller of the transaction which produce this event.
     */
    private String callerAddress;

    /**
     * origin address of the contract which produce this event.
     */
    private String originAddress;

    /**
     * caller address of the contract which produce this event.
     */
    private String creatorAddress;

    /**
     * block number of the transaction
     */
    private Long blockNumber;

    private String blockHash;

    /**
     * true if the transaction has been revoked
     */
    private boolean removed;

    private long latestSolidifiedBlockNumber;

    // private LogInfo logInfo;

    private RawData rawData;

    @Data
    public static class RawData {
        private String address;
        private String[] topics;
        private String data;
    }
}
