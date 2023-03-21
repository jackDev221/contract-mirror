package org.tron.sunio.tronsdk;

import cn.hutool.core.util.HexUtil;
import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import org.tron.sunio.tronsdk.common.crypto.ECKey;
import org.tron.sunio.tronsdk.common.crypto.Sha256Sm3Hash;
import org.tron.sunio.tronsdk.protos.Protocol;
import org.bouncycastle.jcajce.provider.digest.SHA256;

public class TransactionSignUtil {

    public static Protocol.Transaction signTransaction(Protocol.Transaction transaction, byte[] privateKey) {
        ECKey eckey = ECKey.fromPrivate(privateKey);
        byte[] rawData = transaction.getRawData().toByteArray();
        byte[] hash = Sha256Sm3Hash.hash(rawData);
        byte[] sign = eckey.sign(hash).toByteArray();
        return transaction.toBuilder().addSignature(ByteString.copyFrom(sign)).build();
    }

    public static Protocol.Transaction signTransaction(Protocol.Transaction transaction, ECKey eckey) {
        byte[] rawData = transaction.getRawData().toByteArray();
        byte[] hash = Sha256Sm3Hash.hash(rawData);
        byte[] sign = eckey.sign(hash).toByteArray();
        return transaction.toBuilder().addSignature(ByteString.copyFrom(sign)).build();
    }

    public static Protocol.Transaction signTransaction(byte[] txBytes, byte[] privateKey) throws InvalidProtocolBufferException {
        Protocol.Transaction transaction = Protocol.Transaction.parseFrom(txBytes);
        ECKey eckey = ECKey.fromPrivate(privateKey);
        byte[] rawData = transaction.getRawData().toByteArray();
        byte[] hash = Sha256Sm3Hash.hash(rawData);
        byte[] sign = eckey.sign(hash).toByteArray();
        return transaction.toBuilder().addSignature(ByteString.copyFrom(sign)).build();
    }

    public static Protocol.Transaction signTransaction(byte[] txBytes, ECKey eckey) throws InvalidProtocolBufferException {
        Protocol.Transaction transaction = Protocol.Transaction.parseFrom(txBytes);
        byte[] rawData = transaction.getRawData().toByteArray();
        byte[] hash = Sha256Sm3Hash.hash(rawData);
        byte[] sign = eckey.sign(hash).toByteArray();
        return transaction.toBuilder().addSignature(ByteString.copyFrom(sign)).build();
    }

    public static String calculateTransactionHash (Protocol.Transaction txn) {
        SHA256.Digest digest = new SHA256.Digest();
        digest.update(txn.getRawData().toByteArray());
        byte[] txid = digest.digest();
        return "0x" + HexUtil.encodeHexStr(txid);
    }
}
