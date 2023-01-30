package org.tron.sunio.contract_mirror.mirror.chainHelper;

import cn.hutool.core.util.HexUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.tron.sunio.tronsdk.TransactionGenerateUtil;
import org.tron.sunio.tronsdk.TronGrpcClient;
import org.tron.sunio.tronsdk.WalletUtil;
import org.tron.sunio.tronsdk.api.GrpcAPI;
import org.tron.sunio.tronsdk.protos.Protocol;
import org.tron.sunio.tronsdk.protos.contract.SmartContractOuterClass;
import org.web3j.abi.FunctionEncoder;
import org.web3j.abi.FunctionReturnDecoder;
import org.web3j.abi.datatypes.Function;
import org.web3j.abi.datatypes.Type;
import org.web3j.utils.Numeric;

import java.math.BigInteger;
import java.util.Collections;
import java.util.List;

@Slf4j
@Component
public class TronChainHelper implements IChainHelper {
    @Autowired
    @Qualifier("mirrorTronClient")
    private TronGrpcClient tronGrpcClient;

    @Override
    public long blockNumber() {
        GrpcAPI.BlockExtention blockExtention = tronGrpcClient.getNowBlock();
        return blockExtention.getBlockHeader().getRawData().getNumber();
    }

    @Override
    public BigInteger balance(String address) {
        Protocol.Account account = tronGrpcClient.queryAccount(WalletUtil.tronAddress(address));
        return BigInteger.valueOf(account.getBalance());
    }

    @Override
    public List<Type> triggerConstantContract(TriggerContractInfo triggerContractInfo) {
        Function function = triggerContractInfo.getCallFunction();
        byte[] callData = HexUtil.decodeHex(Numeric.cleanHexPrefix(FunctionEncoder.encode(function)));
        SmartContractOuterClass.TriggerSmartContract triggerSmartContract = TransactionGenerateUtil.buildConstCallContract(
                WalletUtil.tronAddress(triggerContractInfo.getFromAddress()),
                WalletUtil.tronAddress(triggerContractInfo.getContractAddress()),
                callData
        );
        GrpcAPI.TransactionExtention response = tronGrpcClient.callWithoutBroadcast(triggerSmartContract);
        String code = response.getResult().getCode().toString();
        if (!code.equalsIgnoreCase("SUCCESS")) {
            log.warn("Call contract failed, code:{}, info:{}", code, triggerContractInfo.toString());
            return Collections.emptyList();
        }
        String result = Numeric.toHexString(response.getConstantResult(0).toByteArray());
        List<Type> results =
                FunctionReturnDecoder.decode(result, function.getOutputParameters());
        return results;
    }
}
