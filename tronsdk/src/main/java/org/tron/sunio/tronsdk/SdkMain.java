package org.tron.sunio.tronsdk;

import cn.hutool.core.util.HexUtil;
import com.google.protobuf.InvalidProtocolBufferException;
import org.tron.sunio.tronsdk.api.GrpcAPI;
import org.tron.sunio.tronsdk.common.crypto.Sha256Sm3Hash;
import org.tron.sunio.tronsdk.protos.Protocol;
import org.tron.sunio.tronsdk.protos.contract.SmartContractOuterClass;
import org.web3j.abi.FunctionEncoder;
import org.web3j.abi.FunctionReturnDecoder;
import org.web3j.abi.TypeReference;
import org.web3j.abi.datatypes.Address;
import org.web3j.abi.datatypes.Function;
import org.web3j.abi.datatypes.Type;
import org.web3j.abi.datatypes.Utf8String;
import org.web3j.abi.datatypes.generated.Uint256;
import org.web3j.utils.Numeric;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

public class SdkMain {

    public static TronGrpcClient client;

    static {
        client = TronGrpcClient.ofShasta();
    }

    public static void main(String[] args) throws InvalidProtocolBufferException, InterruptedException {
        getTokenName();
        getTokenSymbol();
        getTokenBalance();

        //查询账号数据
        Protocol.Account account = client.queryAccount(WalletUtil.tronAddress("TDnJBf1iC9jtfv2CFTqGnNAepPfs9yqBmT"));
        System.out.println(account.getBalance());
        // 以下为常用的几个和波场区块链进行交互的例子
        // 注意: 波场协议中Tapos校验机制要求每个交易都引用某个已经上链的区块，且该被引用区块的范围需要在区间[当前头块高度 - 65536, 当前头块高度]之内。
        // 本SDK将其抽象为ReferBlock类，以下例子力求简明，使用常量将ReferBlock进行构造。实际测试或者使用时，应首先查询链上的最近区块信息，进而动态构造。
        // 或者使用TronGrpcClient中的availableReferBlock方法

        // 1.转账Trx
        // transfer();

        // 2.调用智能合约
        triggerContract();

//         3.调用智能合约的view方法（波场称为Constant方法）， 待补充

        // 4.查询交易
        getTransactionByTxId();

        // 5.查询交易结果 （相当于Eth中的receipt）
        getTransactionInfoByTxId();

        // 6.根据区块高度查询区块
        getBlockByNumber();

        // 7.查询当前头块
        getNowBlock();
    }

    public static void transfer() throws InvalidProtocolBufferException {
        //https://nile.tronscan.org/#/transaction/626408544281b51d3f80bcc1d8f418576d2461bb264cc42290d52976d554d31c
        String addressFrom = "TVFVZJkbk29pPK5BT6gPeGWkwxmGySzmtD";
        String addressTo = "TJCnKsPa7y5okkXvQAidZBzqx3QyQ6sxMW";
        ReferBlock referBlock = ReferBlock.of(HexUtil.decodeHex("ae8323c3412794de"), HexUtil.decodeHex("573f"));
        Protocol.Transaction transaction = TransactionGenerateUtil.createTransferTransaction(WalletUtil.tronAddress(addressFrom), WalletUtil.tronAddress(addressTo), 100,
                referBlock);

        String privateStr = "b5a746decc9a6c9b2baa83e9803e6d5d70ee364d8cede7762fca5c927b114c19";
        byte[] privateBytes = HexUtil.decodeHex(privateStr);

        Protocol.Transaction signedTransaction = TransactionSignUtil.signTransaction(transaction, privateBytes);
        System.out.println(HexUtil.encodeHexStr(signedTransaction.toByteArray()));

        /*
        String bb = HexUtil.encodeHexStr(TransactionSignUtil.signTransaction(transaction.toByteArray(), privateBytes).toByteArray());
        String cc = HexUtil.encodeHexStr(TransactionSignUtil.signTransaction(transaction, ecKey).toByteArray());
        String dd = HexUtil.encodeHexStr(TransactionSignUtil.signTransaction(transaction.toByteArray(), ecKey).toByteArray());
        System.out.println(bb);
        System.out.println(cc);
        System.out.println(dd);
        */

        GrpcAPI.Return response = client.broadcastTransaction(signedTransaction);
        if (response.getResult()) {

        } else {
            System.out.println(response.getCode());
            System.out.println(HexUtil.encodeHexStr(response.getMessage().toByteArray()));
        }

    }

    public static void triggerContract() throws InvalidProtocolBufferException {
        //https://nile.tronscan.org/#/contract/TMgQUjk2mkjMKPN8hAaXVLnf2fiPBm3dDY/code
        String privateStr = "b5a746decc9a6c9b2baa83e9803e6d5d70ee364d8cede7762fca5c927b114c19"; //TVFVZJkbk29pPK5BT6gPeGWkwxmGySzmtD 私钥
        byte[] privateBytes = HexUtil.decodeHex(privateStr);

        String from = "TVFVZJkbk29pPK5BT6gPeGWkwxmGySzmtD";
        String to = "TMgQUjk2mkjMKPN8hAaXVLnf2fiPBm3dDY"; //MetaCoin合约地址
        String sendCoinReceiver = "TJCnKsPa7y5okkXvQAidZBzqx3QyQ6sxMW";

        long callValue = 0;
        List<Type> inputParams = new ArrayList<>();
        inputParams.add(new Address(WalletUtil.ethAddressHex(sendCoinReceiver)));
        inputParams.add(new Uint256(1));
        byte[] data = HexUtil.decodeHex(Numeric.cleanHexPrefix(FunctionEncoder.encode(new Function("sendCoin", inputParams, Collections.<TypeReference<?>>emptyList()))));

        System.out.println("data:" + HexUtil.encodeHexStr(data));
        long callTokenValue = 0;
        long tokenId = 0;
        long expiration = System.currentTimeMillis() + 28800 * 1000;
        long feeLimit = 10 * 1000000;
        ReferBlock referBlock = ReferBlock.of(HexUtil.decodeHex("ae8323c3412794de"), HexUtil.decodeHex("573f"));
        //referBlock = client.availableReferBlock();
        Protocol.Transaction transaction = TransactionGenerateUtil.createTriggerSmartContractTransaction(
                WalletUtil.tronAddress(from),
                WalletUtil.tronAddress(to),
                callValue, data, callTokenValue, tokenId, expiration, feeLimit, referBlock);

        Protocol.Transaction signedTransaction = TransactionSignUtil.signTransaction(transaction, privateBytes);

        System.out.println(HexUtil.encodeHexStr(signedTransaction.toByteArray()));

        GrpcAPI.Return response = client.broadcastTransaction(signedTransaction);
        if (response.getResult()) {

        } else {
            System.out.println(response.getCode());
            System.out.println(HexUtil.encodeHexStr(response.getMessage().toByteArray()));
        }
    }

    public static void getTransactionByTxId() {
        String txId = "626408544281b51d3f80bcc1d8f418576d2461bb264cc42290d52976d554d31c";
        Optional<Protocol.Transaction> optional = client.getTransactionByTxId(txId);
        optional.ifPresent(transaction -> System.out.println(HexUtil.encodeHexStr(transaction.getRawData().toByteArray())));
    }

    public static void getTransactionInfoByTxId() {
        String txId = "0x626408544281b51d3f80bcc1d8f418576d2461bb264cc42290d52976d554d31c";
        Optional<Protocol.TransactionInfo> optional = client.getTransactionInfoByTxId(txId);
        optional.ifPresent(transactionInfo -> System.out.println(transactionInfo.getBlockNumber()));
    }

    public static void getBlockByNumber() {

        GrpcAPI.BlockExtention block = client.getBlockByNumber(0);
        System.out.println("block number: " + block.getBlockHeader().getRawData().getNumber());
    }

    public static void getNowBlock() {
        GrpcAPI.BlockExtention block = client.getNowBlock();
        long blockNumber = block.getBlockHeader().getRawData().getNumber();
        byte[] blockHash = Sha256Sm3Hash.of(block.getBlockHeader().getRawData().toByteArray()).getBytes();
        System.out.println("block number: " + blockNumber);
        System.out.println("block id: " + HexUtil.encodeHexStr(BlockIdUtil.generateBlockId(blockNumber, blockHash)));
    }

    private static void getTokenBalance() {
        String from = "TDsRvUqKv2ESYvdRi51zKb2WjNw9gAF51z";
        String to = "TBpfYqPcKiBU85hRgGVF4ZVvskbaZ4jRGp"; //MetaCoin合约地址
        String methodName = "balanceOf";
        List<Type> inputParameters = new ArrayList<>();
        List<TypeReference<?>> outputParameters = new ArrayList<>();
        Address addr = new Address(WalletUtil.ethAddressHex(from));
        inputParameters.add(addr);

        TypeReference<Uint256> typeReference = new TypeReference<>() {
        };
        outputParameters.add(typeReference);
        Function function = new Function(methodName, inputParameters, outputParameters);
        byte[] data = HexUtil.decodeHex(Numeric.cleanHexPrefix(FunctionEncoder.encode(function)));
        SmartContractOuterClass.TriggerSmartContract triggerSmartContract = TransactionGenerateUtil.buildConstCallContract(
                WalletUtil.tronAddress(from),
                WalletUtil.tronAddress(to),
                data
        );
        GrpcAPI.TransactionExtention response = client.callWithoutBroadcast(triggerSmartContract);

        String result = Numeric.toHexString(response.getConstantResult(0).toByteArray());
        List<Type> results =
                FunctionReturnDecoder.decode(result, function.getOutputParameters());
        BigInteger balance = (BigInteger) results.get(0).getValue();
        System.out.println(balance.toString());

    }

    private static void getTokenName() {
        String from = "TDnJBf1iC9jtfv2CFTqGnNAepPfs9yqBmT";
        String to = "TBpfYqPcKiBU85hRgGVF4ZVvskbaZ4jRGp"; //MetaCoin合约地址
        String methodName = "name";
        List<Type> inputParameters = new ArrayList<>();
        List<TypeReference<?>> outputParameters = new ArrayList<>();

        TypeReference<Utf8String> typeReference = new TypeReference<>() {
        };
        outputParameters.add(typeReference);

        Function function = new Function(methodName, inputParameters, outputParameters);

        byte[] data = HexUtil.decodeHex(Numeric.cleanHexPrefix(FunctionEncoder.encode(function)));

        SmartContractOuterClass.TriggerSmartContract triggerSmartContract = TransactionGenerateUtil.buildConstCallContract(
                WalletUtil.tronAddress(from),
                WalletUtil.tronAddress(to),
                data
        );
        GrpcAPI.TransactionExtention response = client.callWithoutBroadcast(triggerSmartContract);

        String result = Numeric.toHexString(response.getConstantResult(0).toByteArray());
        List<Type> results =
                FunctionReturnDecoder.decode(result, function.getOutputParameters());

        String name = results.get(0).getValue().toString();
        System.out.println("Name:" + name);
    }

    private static void getTokenSymbol(){
        String from = "TDnJBf1iC9jtfv2CFTqGnNAepPfs9yqBmT";
        String to = "TBpfYqPcKiBU85hRgGVF4ZVvskbaZ4jRGp"; //MetaCoin合约地址
        String methodName = "symbol";

        List<Type> inputParameters = new ArrayList<>();
        List<TypeReference<?>> outputParameters = new ArrayList<>();

        TypeReference<Utf8String> typeReference = new TypeReference<>() {
        };
        outputParameters.add(typeReference);

        Function function = new Function(methodName, inputParameters, outputParameters);
        byte[] data = HexUtil.decodeHex(Numeric.cleanHexPrefix(FunctionEncoder.encode(function)));

        SmartContractOuterClass.TriggerSmartContract triggerSmartContract = TransactionGenerateUtil.buildConstCallContract(
                WalletUtil.tronAddress(from),
                WalletUtil.tronAddress(to),
                data
        );
        GrpcAPI.TransactionExtention response = client.callWithoutBroadcast(triggerSmartContract);

        String  result = Numeric.toHexString(response.getConstantResult(0).toByteArray());
        List<Type> results =
                FunctionReturnDecoder.decode(result, function.getOutputParameters());
        String symbol = results.get(0).getValue().toString();
        System.out.println("Symbol: " + symbol);
    }


}
