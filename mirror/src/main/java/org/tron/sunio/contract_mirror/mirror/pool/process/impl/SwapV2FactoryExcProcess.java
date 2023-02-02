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
import org.tron.sunio.contract_mirror.mirror.pool.process.out.SwapV2FactoryExOut;
import org.tron.sunio.contract_mirror.mirror.tools.EthUtil;
import org.tron.sunio.tronsdk.WalletUtil;
import org.web3j.abi.TypeReference;
import org.web3j.abi.datatypes.Address;
import org.web3j.abi.datatypes.Type;
import org.web3j.abi.datatypes.generated.Uint256;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Slf4j
@Component
public class SwapV2FactoryExcProcess implements IProcessor {
    @Autowired
    protected IChainHelper iChainHelper;

    @Override
    public Pair<String, BaseProcessOut> getOnlineData(BaseProcessIn in) {
        if (in instanceof SwapFactoryExIn) {
            try {
                SwapFactoryExIn swapFactoryExIn = (SwapFactoryExIn) in;
                Address pairAddress = getPairWithId(swapFactoryExIn.getId(), swapFactoryExIn.getAddress());
                String token0 = WalletUtil.ethAddressToTron(callContractAddress(ContractMirrorConst.EMPTY_ADDRESS, in.getAddress(), "token0").toString());
                String token1 = WalletUtil.ethAddressToTron(callContractAddress(ContractMirrorConst.EMPTY_ADDRESS, in.getAddress(), "token1").toString());
                SwapV2FactoryExOut out = new SwapV2FactoryExOut();
                out.setOutKey(swapFactoryExIn.getOutKey());
                out.setId(swapFactoryExIn.getId());
                out.setAddress(WalletUtil.ethAddressToTron(pairAddress.toString()));
                out.setToken0(token0);
                out.setToken1(token1);
                return new Pair<>(out.getOutKey(), out);
            } catch (Exception e) {
            }
        }
        return null;
    }

    private Address getPairWithId(long id, String from) {
        List<Type> inputParameters = new ArrayList<>();
        inputParameters.add(new Uint256(id));
        List<TypeReference<?>> outputParameters = new ArrayList<>();
        outputParameters.add(new TypeReference<Address>() {
        });
        TriggerContractInfo triggerContractInfo = new TriggerContractInfo(
                ContractMirrorConst.EMPTY_ADDRESS,
                from,
                "allPairs",
                inputParameters,
                outputParameters
        );
        triggerContractInfo.setOutputParameters(outputParameters);
        List<Type> results = this.iChainHelper.triggerConstantContract(triggerContractInfo);
        if (results.size() > 0) {
            return new Address(EthUtil.addHexPrefix((String) results.get(0).getValue()));
        }
        return Address.DEFAULT;
    }

    protected Address callContractAddress(String from, String to, String method) {
        TriggerContractInfo triggerContractInfo = new TriggerContractInfo(from, to, method, Collections.EMPTY_LIST,
                List.of(new TypeReference<Address>() {
                })
        );
        List<Type> results = this.iChainHelper.triggerConstantContract(triggerContractInfo);
        if (results.size() == 0) {
            log.error("Get contract:{} type:SwapV2FactoryExcProcess , function:{} result len is zero", to, method);
            return Address.DEFAULT;
        }
        return new Address(EthUtil.addHexPrefix((String) results.get(0).getValue()));
    }

}
