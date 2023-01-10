package org.tron.sunio.tronsdk;

import cn.hutool.core.lang.Assert;
import cn.hutool.core.util.HexUtil;
import cn.hutool.core.util.StrUtil;
import com.google.protobuf.ByteString;

import org.tron.sunio.tronsdk.api.GrpcAPI;
import org.tron.sunio.tronsdk.api.WalletGrpc;
import org.tron.sunio.tronsdk.api.WalletSolidityGrpc;
import org.tron.sunio.tronsdk.protos.Protocol;
import org.tron.sunio.tronsdk.protos.contract.SmartContractOuterClass;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import lombok.extern.slf4j.Slf4j;


import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;


@Slf4j
public class TronGrpcClient {

    public final WalletGrpc.WalletBlockingStub blockingStub;
    public final WalletSolidityGrpc.WalletSolidityBlockingStub blockingStubSolidity;
    public final ManagedChannel channel;
    public final ManagedChannel channelSolidity;

    private ScheduledExecutorService scheduledExecutorService;

    //The "side effect" of long connection keep-alive, convenient to use when constructing transactions
    //see the function "availableReferBlock"
    private  volatile  ReferBlock referBlock = null;

    private TronGrpcClient(String grpcEndpoint, String grpcEndpointSolidity) {
        channel = ManagedChannelBuilder.forTarget(grpcEndpoint).usePlaintext().build();
        channelSolidity = ManagedChannelBuilder.forTarget(grpcEndpointSolidity).usePlaintext().build();
        blockingStub = WalletGrpc.newBlockingStub(channel);
        blockingStubSolidity = WalletSolidityGrpc.newBlockingStub(channelSolidity);
    }

    public static TronGrpcClient ofNetwork(String grpcEndpoint, String grpcEndpointSolidity) {
        return new TronGrpcClient(grpcEndpoint, grpcEndpointSolidity).startKeepAlive();
    }

    public static TronGrpcClient ofMainNet() {
        return new TronGrpcClient("grpc.trongrid.io:50051", "grpc.trongrid.io:50052").startKeepAlive();
    }

    public static TronGrpcClient ofShasta() {
        return new TronGrpcClient("grpc.shasta.trongrid.io:50051", "grpc.shasta.trongrid.io:50052").startKeepAlive();
    }

    public static TronGrpcClient ofNile() {
        return new TronGrpcClient("47.252.19.181:50051", "47.252.19.181:50061").startKeepAlive();
    }

    public void shutdown() throws InterruptedException {
        channel.shutdown().awaitTermination(5, TimeUnit.SECONDS);
        channelSolidity.shutdown().awaitTermination(5, TimeUnit.SECONDS);
    }

    public ReferBlock availableReferBlock() {
        return referBlock;
    }

    // for long connection keep-alive
    private TronGrpcClient startKeepAlive() {
        Runnable runnable = () -> {
            try {
                GrpcAPI.BlockExtention blockExtention = this.getNowBlock();
                //long blockTimestamp = blockExtention.getBlockHeader().getRawData().getTimestamp(); // for block timestamp valid checking
                referBlock = ReferBlock.of(blockExtention);
                log.debug("keep alive task run success. block number: {}", blockExtention.getBlockHeader().getRawData().getNumber());
            } catch (Exception e) {
                log.error("keep alive task run failure.", e);
            }
        };

        // invoke once when startup.
        runnable.run();
        if (Objects.isNull(referBlock)) {
            log.error("refer block is null, please check the connection of rpc service");
            throw new IllegalStateException("Tron Grpc Client startup failure");
        }
        log.info("refer block available. block hash: {}, block number: {}",
                HexUtil.encodeHexStr(referBlock.getRefBlockHash()),
                Integer.parseInt(HexUtil.encodeHexStr(referBlock.getRefBlockNum()), 16));


        // schedule task for keep alive.
        scheduledExecutorService = Executors.newScheduledThreadPool(1);
        scheduledExecutorService.scheduleWithFixedDelay(runnable, 30, 30, TimeUnit.SECONDS);
        return this;
    }

    public Optional<Protocol.Transaction> getTransactionById(String txID) {
        GrpcAPI.BytesMessage request = GrpcAPI.BytesMessage.newBuilder().setValue(ByteString.copyFrom(HexUtil.decodeHex(txID))).build();
        Protocol.Transaction transaction;
        transaction = blockingStub.getTransactionById(request);
        return Optional.ofNullable(transaction);
    }

    public Optional<Protocol.TransactionInfo> getTransactionInfoById(String txID) {
        GrpcAPI.BytesMessage request = GrpcAPI.BytesMessage.newBuilder().setValue(ByteString.copyFrom(HexUtil.decodeHex(txID))).build();
        Protocol.TransactionInfo transactionInfo;
        transactionInfo = blockingStub.getTransactionInfoById(request);
        return Optional.ofNullable(transactionInfo);
    }

    public Protocol.Account queryAccount(byte[] address) {
        Protocol.Account request = Protocol.Account.newBuilder().setAddress(ByteString.copyFrom(address)).build();
        return blockingStub.getAccount(request);
    }

    public Protocol.Account queryAccountById(String accountId) {
        ByteString bsAccountId = ByteString.copyFromUtf8(accountId);
        Protocol.Account request = Protocol.Account.newBuilder().setAccountId(bsAccountId).build();
        return blockingStub.getAccountById(request);
    }

    public GrpcAPI.Return broadcastTransaction(Protocol.Transaction signedTransaction) {
        GrpcAPI.Return response = blockingStub.broadcastTransaction(signedTransaction);
        int i = 10;
        while (!response.getResult() && response.getCode() == GrpcAPI.Return.response_code.SERVER_BUSY
                && i > 0) {
            i--;
            response = blockingStub.broadcastTransaction(signedTransaction);
            try {
                Thread.sleep(1000);
            } catch (InterruptedException ignore) {
            }
        }
        return response;
    }

    public Optional<Protocol.Transaction> getTransactionByTxId(String txId) {
        txId = validateAndFormat(txId);
        ByteString bsTxId = ByteString.copyFrom(HexUtil.decodeHex(txId));
        GrpcAPI.BytesMessage request = GrpcAPI.BytesMessage.newBuilder().setValue(bsTxId).build();
        Protocol.Transaction transaction;
        transaction = blockingStub.getTransactionById(request);
        return Optional.ofNullable(transaction);
    }

    public Optional<Protocol.TransactionInfo> getTransactionInfoByTxId(String txId) {
        txId = validateAndFormat(txId);
        ByteString bsTxId = ByteString.copyFrom(HexUtil.decodeHex(txId));
        GrpcAPI.BytesMessage request = GrpcAPI.BytesMessage.newBuilder().setValue(bsTxId).build();
        Protocol.TransactionInfo transactionInfo;
        transactionInfo = blockingStub.getTransactionInfoById(request);
        return Optional.ofNullable(transactionInfo);
    }

    public Optional<Protocol.Block> getBlockById(String blockID) {
        ByteString bsBlockId = ByteString.copyFrom(HexUtil.decodeHex(blockID));
        GrpcAPI.BytesMessage request = GrpcAPI.BytesMessage.newBuilder().setValue(bsBlockId).build();
        Protocol.Block block = blockingStub.getBlockById(request);
        return Optional.ofNullable(block);
    }

    public GrpcAPI.BlockExtention getBlockByNumber(long blockNum) {
        Assert.isTrue(blockNum >= 0, "invalid blockNumber");
        GrpcAPI.NumberMessage.Builder builder = GrpcAPI.NumberMessage.newBuilder();
        builder.setNum(blockNum);
        return blockingStub.getBlockByNum2(builder.build());
    }

    public GrpcAPI.BlockExtention getNowBlock() {
        return blockingStub.getNowBlock2(GrpcAPI.EmptyMessage.newBuilder().build());
    }

    private String validateAndFormat(String txId) {
        Assert.isTrue(StrUtil.isNotBlank(txId), "txId can not be null");
        if (txId.startsWith("0x") || txId.startsWith("0X")) {
            txId = txId.substring(2);
        }
        Assert.isTrue(txId.length() == 64, "txId length must be 64");
        return txId;
    }

    public GrpcAPI.TransactionExtention callWithoutBroadcast(SmartContractOuterClass.TriggerSmartContract request) {
        return blockingStub.triggerConstantContract(request);
    }
}
