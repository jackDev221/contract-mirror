package org.tron.sunio.contract_mirror.mirror.pool.process.impl;

import cn.hutool.core.lang.Pair;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.tron.sunio.contract_mirror.mirror.chainHelper.IChainHelper;
import org.tron.sunio.contract_mirror.mirror.chainHelper.TriggerContractInfo;
import org.tron.sunio.contract_mirror.mirror.consts.ContractMirrorConst;
import org.tron.sunio.contract_mirror.mirror.pool.process.IProcessor;
import org.tron.sunio.contract_mirror.mirror.pool.process.in.BaseProcessIn;
import org.tron.sunio.contract_mirror.mirror.pool.process.in.SwapFactoryExIn;
import org.tron.sunio.contract_mirror.mirror.pool.process.out.BaseProcessOut;
import org.tron.sunio.contract_mirror.mirror.pool.process.out.SwapV1FactoryExOut;
import org.tron.sunio.contract_mirror.mirror.tools.EthUtil;
import org.tron.sunio.tronsdk.WalletUtil;
import org.web3j.abi.TypeReference;
import org.web3j.abi.datatypes.Address;
import org.web3j.abi.datatypes.Type;
import org.web3j.abi.datatypes.generated.Uint256;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
public class SwapV1FactoryExcProcess implements IProcessor {
    @Autowired
    protected IChainHelper iChainHelper;

    @Override
    public Pair<String, BaseProcessOut> getOnlineData(BaseProcessIn in) {
        if (in instanceof SwapFactoryExIn) {
            try {
                SwapFactoryExIn swapFactoryExIn = (SwapFactoryExIn) in;
                SwapV1FactoryExOut swapV1FactoryExOut = new SwapV1FactoryExOut();
                swapV1FactoryExOut.setOutKey(swapFactoryExIn.getOutKey());
                Address tokenAddress = getTokenWithId(swapFactoryExIn.getId(), swapFactoryExIn.getAddress());
                Address contractAddress = getExchange(tokenAddress, (in.getAddress()));
                swapV1FactoryExOut.setId(swapFactoryExIn.getId());
                swapV1FactoryExOut.setAddress(WalletUtil.ethAddressToTron(contractAddress.toString()));
                swapV1FactoryExOut.setTokenAddress(WalletUtil.ethAddressToTron(tokenAddress.toString()));
                return new Pair<>(swapV1FactoryExOut.getOutKey(), swapV1FactoryExOut);
            } catch (Exception e) {
            }
        }
        return null;
    }

    private Address getTokenWithId(long id, String address) {
        List<Type> inputParameters = new ArrayList<>();
        inputParameters.add(new Uint256(id));
        List<TypeReference<?>> outputParameters = new ArrayList<>();
        outputParameters.add(new TypeReference<Address>() {
        });
        TriggerContractInfo triggerContractInfo = new TriggerContractInfo(
                ContractMirrorConst.EMPTY_ADDRESS,
                address,
                "getTokenWithId",
                inputParameters,
                outputParameters
        );
        triggerContractInfo.setOutputParameters(outputParameters);
        List<Type> results = this.iChainHelper.triggerConstantContract(triggerContractInfo);
        if (results.size() > 0) {
            return new Address(EthUtil.addHexPrefix((String) results.get(0).getValue()));
        }
        log.error("Contract:{}, type:SwapV1FactoryExcProcess  call getTokenWithId:{} failed", address, id);
        return Address.DEFAULT;
    }

    public Address getExchange(Address tokenAddress, String from) {
        List<Type> inputParameters = new ArrayList<>();
        inputParameters.add(tokenAddress);
        List<TypeReference<?>> outputParameters = new ArrayList<>();
        outputParameters.add(new TypeReference<Address>() {
        });
        TriggerContractInfo triggerContractInfo = new TriggerContractInfo(
                ContractMirrorConst.EMPTY_ADDRESS,
                from,
                "getExchange",
                inputParameters,
                outputParameters
        );
        List<Type> results = this.iChainHelper.triggerConstantContract(triggerContractInfo);
        if (results.size() == 0) {
            log.error("Contract:{}, type:SwapV1FactoryExcProcess call getExchange:{} failed", from, tokenAddress);
            return Address.DEFAULT;
        }
        return new Address(EthUtil.addHexPrefix((String) results.get(0).getValue()));
    }
}
