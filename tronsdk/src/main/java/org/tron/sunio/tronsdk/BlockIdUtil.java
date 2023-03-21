package org.tron.sunio.tronsdk;

import com.google.common.primitives.Longs;
import org.tron.sunio.tronsdk.common.crypto.Sha256Sm3Hash;
import org.tron.sunio.tronsdk.protos.Protocol;

public class BlockIdUtil {

    //Tron协议中的blockId是block hash同block number的结合体
    //block hash是对block header 的RawData计算sha256哈希
    //blockId的生成规则： 将block hash的前8个字节用block number进行替换

    public static byte[] generateBlockId(Protocol.Block block) {
        byte[] blockHash = Sha256Sm3Hash.of(block.getBlockHeader().getRawData().toByteArray()).getBytes();
        long blockNumber = block.getBlockHeader().getRawData().getNumber();
        return generateBlockId(blockNumber, blockHash);
    }

    public static byte[] generateBlockId(long blockNum, Sha256Sm3Hash blockHash) {
        byte[] numBytes = Longs.toByteArray(blockNum);
        byte[] hash = new byte[blockHash.getBytes().length];
        System.arraycopy(numBytes, 0, hash, 0, 8);
        System.arraycopy(blockHash.getBytes(), 8, hash, 8, blockHash.getBytes().length - 8);
        return hash;
    }

    public static byte[] generateBlockId(long blockNum, byte[] blockHash) {
        byte[] numBytes = Longs.toByteArray(blockNum);
        byte[] hash = new byte[blockHash.length];
        System.arraycopy(numBytes, 0, hash, 0, 8);
        System.arraycopy(blockHash, 8, hash, 8, blockHash.length - 8);
        return hash;
    }
}
