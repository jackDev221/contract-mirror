package org.tron.defi.contract_mirror.dao;

import lombok.Data;

@Data
public class BlockInfo {
    private final long blockNumber;
    private final String blockHash;

    public BlockInfo(long blockNumber, String blockHash) {
        this.blockNumber = blockNumber;
        this.blockHash = blockHash;
    }

    public boolean isConflict(BlockInfo blockInfo) {
        return getBlockNumber() == blockInfo.getBlockNumber() && !isEqual(blockInfo);
    }

    public boolean isEarlier(BlockInfo blockInfo) {
        return getBlockNumber() < blockInfo.getBlockNumber();
    }

    public boolean isEqual(BlockInfo blockInfo) {
        return getBlockNumber() == blockInfo.getBlockNumber() &&
               getBlockHash().equals(blockInfo.getBlockHash());
    }
}
