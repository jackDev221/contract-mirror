package org.tron.sunio.contract_mirror.mirror.contracts.factory;

import cn.hutool.core.util.ObjectUtil;
import lombok.extern.slf4j.Slf4j;
import org.tron.sunio.contract_mirror.event_decode.events.SwapV2PairEvent;
import org.tron.sunio.contract_mirror.mirror.chainHelper.IChainHelper;
import org.tron.sunio.contract_mirror.mirror.chainHelper.TriggerContractInfo;
import org.tron.sunio.contract_mirror.mirror.consts.ContractMirrorConst;
import org.tron.sunio.contract_mirror.mirror.contracts.BaseContract;
import org.tron.sunio.contract_mirror.mirror.contracts.IContractFactory;
import org.tron.sunio.contract_mirror.mirror.contracts.events.IContractEventWrap;
import org.tron.sunio.contract_mirror.mirror.contracts.impl.SwapV2Pair;
import org.tron.sunio.contract_mirror.mirror.dao.SwapFactoryV2Data;
import org.tron.sunio.contract_mirror.mirror.db.IDbHandler;
import org.tron.sunio.contract_mirror.mirror.enums.ContractType;
import org.tron.sunio.contract_mirror.mirror.tools.EthUtil;
import org.tron.sunio.tronsdk.WalletUtil;
import org.web3j.abi.TypeReference;
import org.web3j.abi.datatypes.Address;
import org.web3j.abi.datatypes.Type;
import org.web3j.abi.datatypes.generated.Uint256;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.tron.sunio.contract_mirror.event_decode.events.SwapV2FactoryEvent.EVENT_NAME_PAIR_CREATED_MINT;

@Slf4j
public class SwapFactoryV2 extends BaseContract implements IContractFactory {
    private Map<String, String> v2PairSigMap;

    public SwapFactoryV2(String address, IChainHelper iChainHelper, IDbHandler iDbHandler,
                         Map<String, String> sigMap) {
        super(address, ContractType.SWAP_FACTORY_V2, iChainHelper, iDbHandler, sigMap);
        v2PairSigMap = SwapV2PairEvent.getSigMap();
    }

    @Override
    public BaseContract getBaseContract() {
        return this;
    }

    private Address getPairWithId(long id) {
        List<Type> inputParameters = new ArrayList<>();
        inputParameters.add(new Uint256(id));
        List<TypeReference<?>> outputParameters = new ArrayList<>();
        outputParameters.add(new TypeReference<Address>() {
        });
        TriggerContractInfo triggerContractInfo = new TriggerContractInfo(
                ContractMirrorConst.EMPTY_ADDRESS,
                this.getAddress(),
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

    @Override
    public List<BaseContract> getListContracts() {
        List<BaseContract> result = new ArrayList<>();
        long pairCount = getPairCount().longValue();
        for (long i = 0; i < pairCount; i++) {
            Address pairAddress = getPairWithId(i);
            if (pairAddress.equals(Address.DEFAULT)) {
                continue;
            }
            SwapV2Pair swapV2Pair = new SwapV2Pair(
                    WalletUtil.ethAddressToTron(pairAddress.toString()),
                    this.address,
                    this.iChainHelper,
                    this.iDbHandler,
                    v2PairSigMap

            );
            result.add(swapV2Pair);
        }
        return result;
    }

    @Override
    public List<String> getListContractAddresses() {
        List<String> result = new ArrayList<>();
        long pairCount = getPairCount().longValue();
        for (long i = 0; i < pairCount; i++) {
            Address pairAddress = getPairWithId(i);
            if (pairAddress.equals(Address.DEFAULT)) {
                continue;
            }
            result.add(WalletUtil.ethAddressToTron(pairAddress.toString()));
        }
        return result;
    }

    @Override
    public void updateBaseInfoToCache(boolean isUsing, boolean isReady, boolean isAddExchangeContracts) {
        SwapFactoryV2Data factoryV2Data = iDbHandler.querySwapFactoryV2Data(this.address);
        factoryV2Data.setUsing(isUsing);
        factoryV2Data.setReady(isReady);
        factoryV2Data.setAddExchangeContracts(isAddExchangeContracts);
        iDbHandler.updateSwapFactoryV2Data(factoryV2Data);
    }

    @Override
    public String getFactoryState() {
        SwapFactoryV2Data factoryV2Data = iDbHandler.querySwapFactoryV2Data(this.address);
        if (ObjectUtil.isNotNull(factoryV2Data)) {
            return String.format("Address:%s, Type: %s, feeTo:%s, feeToSetter:%d", this.address, this.type, factoryV2Data.getFeeTo(),
                    factoryV2Data.getFeeToSetter());
        }
        return String.format("Address:%s, Type: %s not init", this.address, this.type);
    }

    private BigInteger getPairCount() {
        List<Type> inputParameters = new ArrayList<>();
        List<TypeReference<?>> outputParameters = new ArrayList<>();
        outputParameters.add(new TypeReference<Uint256>() {
        });
        TriggerContractInfo triggerContractInfo = new TriggerContractInfo(
                ContractMirrorConst.EMPTY_ADDRESS,
                this.getAddress(),
                "allPairsLength",
                inputParameters,
                outputParameters
        );
        List<Type> results = this.iChainHelper.triggerConstantContract(triggerContractInfo);
        if (results.size() == 0){
            log.error("Contract:{}, type: {} call tokenCount failed", address, type);
            return BigInteger.ZERO;
        }
        return (BigInteger) results.get(0).getValue();
    }

    private void callChainData(SwapFactoryV2Data factoryV2Data) {
        try {
            TriggerContractInfo triggerContractInfo = new TriggerContractInfo();
            triggerContractInfo.setContractAddress(this.getAddress());
            triggerContractInfo.setFromAddress(ContractMirrorConst.EMPTY_ADDRESS);
            triggerContractInfo.setMethodName("feeTo");
            List<Type> inputParameters = new ArrayList<>();
            triggerContractInfo.setInputParameters(inputParameters);
            List<TypeReference<?>> outputParameters = new ArrayList<>();
            outputParameters.add(new TypeReference<Address>() {
            });
            triggerContractInfo.setOutputParameters(outputParameters);
            List<Type> results = this.iChainHelper.triggerConstantContract(triggerContractInfo);
            if (results.size() > 0) {
                Address feeAddress = (Address) results.get(0).getValue();
                factoryV2Data.setFeeTo(WalletUtil.ethAddressToTron(feeAddress.toString()));
            }
            triggerContractInfo.setMethodName("feeToSetter()");
            results = this.iChainHelper.triggerConstantContract(triggerContractInfo);
            if (results.size() > 0) {
                Address feeToSetterAddress = (Address) results.get(0).getValue();
                factoryV2Data.setFeeToSetter(WalletUtil.ethAddressToTron(feeToSetterAddress.toString()));
            }
            long pairCount = getPairCount().longValue();
            factoryV2Data.setPairCount(pairCount);

        } catch (Exception e) {
            log.error("Fail to update Factory:{} property!", address);
        }
    }

    @Override
    public boolean initDataFromChain1() {
        SwapFactoryV2Data factoryV2Data = iDbHandler.querySwapFactoryV2Data(this.address);
        if (ObjectUtil.isNull(factoryV2Data)) {
            factoryV2Data = new SwapFactoryV2Data();
            factoryV2Data.setReady(false);
            factoryV2Data.setUsing(true);
            factoryV2Data.setAddress(this.address);
            factoryV2Data.setType(this.type);
        }
        callChainData(factoryV2Data);
        iDbHandler.updateSwapFactoryV2Data(factoryV2Data);
        return true;
    }

    @Override
    public void handleEvent(IContractEventWrap iContractEventWrap) {
        super.handleEvent(iContractEventWrap);
        if (!isReady) {
            return;
        }
        // Do handleEvent
        String eventName = getEventName(iContractEventWrap);
        String[] topics = iContractEventWrap.getTopics();
        switch (eventName) {
            case EVENT_NAME_PAIR_CREATED_MINT:
                handleCreatePair(topics, iContractEventWrap.getData());
                break;
            default:
                log.warn("event:{} not handle", topics[0]);
                break;
        }
    }

    private void handleCreatePair(String[] _topics, String _data) {
        isAddExchangeContracts = false;
        SwapFactoryV2Data factoryV2Data = iDbHandler.querySwapFactoryV2Data(this.address);
        factoryV2Data.setAddExchangeContracts(false);
        factoryV2Data.setPairCount(factoryV2Data.getPairCount() + 1);
        iDbHandler.updateSwapFactoryV2Data(factoryV2Data);
    }
}
