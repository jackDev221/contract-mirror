package org.tron.sunio.tronsdk;

import cn.hutool.core.lang.Assert;
import cn.hutool.core.util.ArrayUtil;
import cn.hutool.core.util.ObjectUtil;
import org.tron.sunio.tronsdk.api.GrpcAPI;
import org.tron.sunio.tronsdk.common.crypto.Sha256Sm3Hash;
import org.tron.sunio.tronsdk.protos.Protocol;
import lombok.Getter;

import java.nio.ByteBuffer;

public class ReferBlock {

    @Getter
    private final byte[] refBlockHash;

    @Getter
    private final byte[] refBlockNum;

    public ReferBlock(byte[] refBlockHash, byte[] refBlockNum) {
        this.refBlockHash = refBlockHash;
        this.refBlockNum = refBlockNum;
    }

    public static ReferBlock of(Protocol.Block block) {
        byte[] blockHash = Sha256Sm3Hash.of(block.getBlockHeader().getRawData().toByteArray()).getBytes();
        long blockNumber = block.getBlockHeader().getRawData().getNumber();
        byte[] refBlockHash = ArrayUtil.sub(blockHash, 8, 16);
        return of(refBlockHash, blockNumber);
    }

    public static ReferBlock of(GrpcAPI.BlockExtention blockExtention) {
        byte[] blockId = blockExtention.getBlockid().toByteArray();
        byte[] refBlockHash = ArrayUtil.sub(blockId, 8, 16);
        byte[] refBlockNum = ArrayUtil.sub(blockId, 6, 8);
        return of(refBlockHash, refBlockNum);
    }

    public static ReferBlock of(byte[] refBlockHash, long blockNumber) {
        byte[] refBlockNum = ArrayUtil.sub(ByteBuffer.allocate(8).putLong(blockNumber).array(), 6, 8);
        return of(refBlockHash, refBlockNum);
    }

    public static ReferBlock of(byte[] refBlockHash, byte[] refBlockNumber) {
        Assert.isTrue(ObjectUtil.isNotNull(refBlockHash) && refBlockHash.length == 8, "invalid block hash");
        Assert.isTrue(ObjectUtil.isNotNull(refBlockNumber) && refBlockNumber.length == 2, "invalid block number");
        return new ReferBlock(refBlockHash, refBlockNumber);
    }
}
