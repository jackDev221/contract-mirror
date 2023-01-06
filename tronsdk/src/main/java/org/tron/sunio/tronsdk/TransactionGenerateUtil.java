package org.tron.sunio.tronsdk;

import cn.hutool.core.lang.Assert;
import com.google.protobuf.Any;
import com.google.protobuf.ByteString;
import org.tron.sunio.tronsdk.protos.Protocol;
import org.tron.sunio.tronsdk.protos.contract.BalanceContract;
import org.tron.sunio.tronsdk.protos.contract.SmartContractOuterClass;

public class TransactionGenerateUtil {

    //波场协议约定交易有效期最长为1天(86400秒)
    //此处设定为80000秒，以尽量避免不同节点间的时间戳差异
    public static final long MAXIMUM_EXPIRATION = 80000 * 1_000L; //单位: 毫秒，

    //转账TRX交易
    public static Protocol.Transaction createTransferTransaction(byte[] from, byte[] to, long amount, ReferBlock referBlock) {
        long expiration = System.currentTimeMillis() + 3600 * 8 * 1000;
        return createTransferTransaction(from, to, amount, expiration, referBlock);
    }

    //转账TRX交易
    public static Protocol.Transaction createTransferTransaction(byte[] from, byte[] to, long amount, long expiration, ReferBlock referBlock) {
        BalanceContract.TransferContract contract = buildTransferContract(from, to, amount);
        Protocol.Transaction.Contract contract1 = buildTransactionContract(contract, Protocol.Transaction.Contract.ContractType.TransferContract, 0);
        return createTransaction(contract1, expiration, referBlock);
    }

    //智能合约交易
    public static Protocol.Transaction createTriggerSmartContractTransaction(byte[] from, byte[] to, long callValue, byte[] data, long callTokenValue, long tokenId, long expiration,
                                                                             long feeLimit, ReferBlock referBlock) {
        SmartContractOuterClass.TriggerSmartContract contract = buildTriggerSmartContract(from, to, callValue, data, callTokenValue, tokenId);
        Protocol.Transaction.Contract contract1 = buildTransactionContract(contract, Protocol.Transaction.Contract.ContractType.TriggerSmartContract, 0);
        return createTransaction(contract1, feeLimit, expiration, referBlock);
    }

    private static Protocol.Transaction.Contract buildTransactionContract(com.google.protobuf.Message message, Protocol.Transaction.Contract.ContractType contractType, int permissionId) {
        return Protocol.Transaction.Contract.newBuilder()
                .setParameter((message instanceof Any ? (Any) message : Any.pack(message)))
                .setType(contractType)
                .setPermissionId(permissionId)
                .build();
    }

    //系统交易基于此方法进行构建
    private static Protocol.Transaction createTransaction(Protocol.Transaction.Contract contract, long expiration, ReferBlock referBlock) {
        Assert.isTrue(contract.getType() != Protocol.Transaction.Contract.ContractType.CreateSmartContract &&
                contract.getType() != Protocol.Transaction.Contract.ContractType.TriggerSmartContract, "unsupported contract of transaction");
        Assert.isTrue(expiration < System.currentTimeMillis() + MAXIMUM_EXPIRATION, "Transaction expiration invalid");

        Protocol.Transaction.Builder transactionBuilder = Protocol.Transaction.newBuilder();
        Protocol.Transaction.raw raw = Protocol.Transaction.raw.newBuilder()
                .addContract(contract)
                .setTimestamp(System.currentTimeMillis())
                .setExpiration(expiration)
                .setRefBlockHash(ByteString.copyFrom(referBlock.getRefBlockHash()))
                .setRefBlockBytes(ByteString.copyFrom(referBlock.getRefBlockNum()))
                .build();
        return transactionBuilder.setRawData(raw).build();
    }

    //智能合约交易基于此方法进行构建
    private static Protocol.Transaction createTransaction(Protocol.Transaction.Contract contract, long feeLimit, long expiration, ReferBlock referBlock) {
        Assert.isTrue(contract.getType() == Protocol.Transaction.Contract.ContractType.CreateSmartContract ||
                contract.getType() == Protocol.Transaction.Contract.ContractType.TriggerSmartContract, "unsupported contract of transaction");
        Assert.isTrue(expiration < System.currentTimeMillis() + MAXIMUM_EXPIRATION, "Transaction expiration invalid");

        Protocol.Transaction.Builder transactionBuilder = Protocol.Transaction.newBuilder();
        Protocol.Transaction.raw raw = Protocol.Transaction.raw.newBuilder()
                .addContract(contract)
                .setTimestamp(System.currentTimeMillis())
                .setExpiration(expiration)
                .setRefBlockHash(ByteString.copyFrom(referBlock.getRefBlockHash()))
                .setRefBlockBytes(ByteString.copyFrom(referBlock.getRefBlockNum()))
                .setFeeLimit(feeLimit)
                .build();
        return transactionBuilder.setRawData(raw).build();
    }

    private static BalanceContract.TransferContract buildTransferContract(byte[] from, byte[] to, long amount) {
        return BalanceContract.TransferContract.newBuilder()
                .setOwnerAddress(ByteString.copyFrom(from))
                .setToAddress(ByteString.copyFrom(to))
                .setAmount(amount).build();
    }

    private static SmartContractOuterClass.TriggerSmartContract buildTriggerSmartContract(byte[] from, byte[] to, long callValue, byte[] data, long callTokenValue, long tokenId) {
        return SmartContractOuterClass.TriggerSmartContract.newBuilder()
                .setOwnerAddress(ByteString.copyFrom(from))
                .setContractAddress(ByteString.copyFrom(to))
                .setCallValue(callValue)
                .setData(ByteString.copyFrom(data))
                .setCallTokenValue(callTokenValue)
                .setTokenId(tokenId)
                .build();
    }

    public static SmartContractOuterClass.TriggerSmartContract buildConstCallContract(byte[] from, byte[] to, byte[] data) {
        return SmartContractOuterClass.TriggerSmartContract.newBuilder()
                .setOwnerAddress(ByteString.copyFrom(from))
                .setContractAddress(ByteString.copyFrom(to))
                .setData(ByteString.copyFrom(data))
                .build();
    }
}