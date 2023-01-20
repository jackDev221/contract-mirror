package org.tron.sunio.contract_mirror.mirror.contracts.factory;

import cn.hutool.core.util.ObjectUtil;
import lombok.extern.slf4j.Slf4j;
import org.tron.sunio.contract_mirror.event_decode.events.SwapV1Event;
import org.tron.sunio.contract_mirror.event_decode.events.SwapV1FactoryEvent;
import org.tron.sunio.contract_mirror.mirror.db.IDbHandler;
import org.tron.sunio.contract_mirror.mirror.chainHelper.IChainHelper;
import org.tron.sunio.contract_mirror.mirror.chainHelper.TriggerContractInfo;
import org.tron.sunio.contract_mirror.mirror.consts.ContractMirrorConst;
import org.tron.sunio.contract_mirror.mirror.contracts.BaseContract;
import org.tron.sunio.contract_mirror.mirror.contracts.IContractFactory;
import org.tron.sunio.contract_mirror.mirror.contracts.impl.SwapV1;
import org.tron.sunio.contract_mirror.mirror.dao.SwapFactoryV1Data;
import org.tron.sunio.contract_mirror.mirror.enums.ContractType;
import org.tron.sunio.contract_mirror.mirror.tools.EthUtil;
import org.tron.sunio.tronsdk.WalletUtil;
import org.web3j.abi.EventValues;
import org.web3j.abi.TypeReference;
import org.web3j.abi.datatypes.Address;
import org.web3j.abi.datatypes.Type;
import org.web3j.abi.datatypes.generated.Uint256;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
public class SwapFactoryV1 extends BaseContract implements IContractFactory {
    private Map<String, String> v1SigMap;
    private SwapFactoryV1Data swapFactoryV1Data;

    public SwapFactoryV1(String address, IChainHelper iChainHelper, IDbHandler iDbHandler, final Map<String, String> sigMap) {
        super(address, ContractType.SWAP_FACTORY_V1, iChainHelper, iDbHandler, sigMap);
        v1SigMap = SwapV1Event.getSigMap();
    }

    private SwapFactoryV1Data getVarFactoryV1Data() {
        if (ObjectUtil.isNull(swapFactoryV1Data)) {
            swapFactoryV1Data = iDbHandler.querySwapFactoryV1Data(this.address);
            if (ObjectUtil.isNull(swapFactoryV1Data)) {
                swapFactoryV1Data = new SwapFactoryV1Data();
                swapFactoryV1Data.setReady(false);
                swapFactoryV1Data.setUsing(true);
                swapFactoryV1Data.setAddress(this.address);
                swapFactoryV1Data.setType(this.type);
            }
        }
        return swapFactoryV1Data;
    }

    @Override
    public boolean initDataFromChain1() {
        SwapFactoryV1Data swapFactoryV1Data = getVarFactoryV1Data();
        // TODO feeTo feeToRate 可读性
        // set feeTo
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
            swapFactoryV1Data.setFeeAddress(WalletUtil.ethAddressToTron(feeAddress.toString()));
        } else {
            log.error("Get contract:{} type:{} , function:feeTo result len is zero", this.address, this.type);
        }
        // set feeToRate
        triggerContractInfo.setMethodName("feeToRate");
        outputParameters = new ArrayList<>();
        outputParameters.add(new TypeReference<Uint256>() {
        });
        triggerContractInfo.setOutputParameters(outputParameters);
        results = this.iChainHelper.triggerConstantContract(triggerContractInfo);
        if (results.size() > 0) {
            BigInteger feeToRate = (BigInteger) results.get(0).getValue();
            swapFactoryV1Data.setFeeToRate(feeToRate.longValue());
        } else {
            log.error("Get contract:{} type:{} , function:feeToRate result len is zero", this.address, this.type);
        }
        long tokenCount = getTokenCount().longValue();
        swapFactoryV1Data.setTokenCount(tokenCount);
        isDirty = true;
        return true;
    }

    @Override
    public BaseContract getBaseContract() {
        return this;
    }

    @Override
    public List<BaseContract> getListContracts() {
        List<BaseContract> result = new ArrayList<>();
        long totalTokens = getTokenCount().longValue();
        //TODO 这里token数目太多了，之后看看是否能优化下
        for (long i = 0; i < totalTokens; i++) {
            Address tokenAddress = getTokenWithId(i);
            if (tokenAddress.equals(Address.DEFAULT)) {
                continue;
            }
            Address contractAddress = getExchange(tokenAddress);
            if (contractAddress.equals(Address.DEFAULT)) {
                continue;
            }
            SwapV1 swapV1 = new SwapV1(WalletUtil.ethAddressToTron(contractAddress.toString()),
                    this.iChainHelper, this.iDbHandler, WalletUtil.ethAddressToTron(tokenAddress.toString()), v1SigMap);

            result.add(swapV1);
        }
        return result;
    }

    @Override
    public List<String> getListContractAddresses() {
        List<String> result = new ArrayList<>();
        long totalTokens = getTokenCount().longValue();
        for (long i = 0; i < totalTokens; i++) {
            Address tokenAddress = getTokenWithId(i);
            if (tokenAddress.equals(Address.DEFAULT)) {
                continue;
            }
            Address contractAddress = getExchange(tokenAddress);
            if (contractAddress.equals(Address.DEFAULT)) {
                continue;
            }
            result.add(WalletUtil.ethAddressToTron(contractAddress.toString()));
        }
        return result;
    }

    @Override
    public String getFactoryState() {
        SwapFactoryV1Data factoryV1Data = this.getVarFactoryV1Data();
        if (ObjectUtil.isNotNull(factoryV1Data)) {
            return String.format("Address:%s, Type: %s, feeAddress:%s, feeToRate:%d", this.address, this.type, factoryV1Data.getFeeAddress(),
                    factoryV1Data.getFeeToRate());
        }
        return String.format("Address:%s, Type: %s not init", this.address, this.type);

    }

    private BigInteger getTokenCount() {
        List<Type> inputParameters = new ArrayList<>();
        List<TypeReference<?>> outputParameters = new ArrayList<>();
        outputParameters.add(new TypeReference<Uint256>() {
        });
        TriggerContractInfo triggerContractInfo = new TriggerContractInfo(
                ContractMirrorConst.EMPTY_ADDRESS,
                this.getAddress(),
                "tokenCount",
                inputParameters,
                outputParameters
        );
        List<Type> results = this.iChainHelper.triggerConstantContract(triggerContractInfo);
        if (results.size() == 0) {
            log.error("Contract:{}, type: {} call tokenCount failed", address, type);
            return BigInteger.ZERO;
        }
        return (BigInteger) results.get(0).getValue();
    }

    public Address getTokenWithId(long id) {
        List<Type> inputParameters = new ArrayList<>();
        inputParameters.add(new Uint256(id));
        List<TypeReference<?>> outputParameters = new ArrayList<>();
        outputParameters.add(new TypeReference<Address>() {
        });
        TriggerContractInfo triggerContractInfo = new TriggerContractInfo(
                ContractMirrorConst.EMPTY_ADDRESS,
                this.getAddress(),
                "getTokenWithId",
                inputParameters,
                outputParameters
        );
        triggerContractInfo.setOutputParameters(outputParameters);
        List<Type> results = this.iChainHelper.triggerConstantContract(triggerContractInfo);
        if (results.size() > 0) {
            return new Address(EthUtil.addHexPrefix((String) results.get(0).getValue()));
        }
        log.error("Contract:{}, type: {} call getTokenWithId:{} failed", address, type, id);
        return Address.DEFAULT;
    }

    public Address getExchange(Address tokenAddress) {
        List<Type> inputParameters = new ArrayList<>();
        inputParameters.add(tokenAddress);
        List<TypeReference<?>> outputParameters = new ArrayList<>();
        outputParameters.add(new TypeReference<Address>() {
        });
        TriggerContractInfo triggerContractInfo = new TriggerContractInfo(
                ContractMirrorConst.EMPTY_ADDRESS,
                this.getAddress(),
                "getExchange",
                inputParameters,
                outputParameters
        );
        List<Type> results = this.iChainHelper.triggerConstantContract(triggerContractInfo);
        if (results.size() == 0) {
            log.error("Contract:{}, type: {} call getExchange:{} failed", address, type, tokenAddress);
            return Address.DEFAULT;
        }
        return new Address(EthUtil.addHexPrefix((String) results.get(0).getValue()));
    }

    @Override
    public void updateBaseInfo(boolean isUsing, boolean isReady, boolean isAddExchangeContracts) {
        SwapFactoryV1Data factoryV1Data = getVarFactoryV1Data();
        factoryV1Data.setReady(isReady);
        factoryV1Data.setUsing(isUsing);
        factoryV1Data.setAddExchangeContracts(isAddExchangeContracts);
        this.isDirty = true;
    }

    @Override
    protected void saveUpdateToCache() {
        SwapFactoryV1Data factoryV1Data = this.getVarFactoryV1Data();
        iDbHandler.updateSwapFactoryV1Data(factoryV1Data);
    }

    @Override
    protected HandleResult handleEvent1(String eventName, String[] topics, String data, HandleEventExtraData handleEventExtraData) {
        HandleResult result;
        switch (eventName) {
            case SwapV1FactoryEvent
                    .EVENT_NAME_FEE_RATE:
                result = handEventFeeRate(topics, data, handleEventExtraData);
                break;
            case SwapV1FactoryEvent
                    .EVENT_NAME_FEE_TO:
                result = handEventFeeTo(topics, data, handleEventExtraData);
                break;
            case SwapV1FactoryEvent
                    .EVENT_NAME_NEW_EXCHANGE:
                result = handEventNewExchange(topics, data);
                break;
            default:
                log.warn("Contract:{} type:{} event:{} not handle", address, type, topics[0]);
                result = HandleResult.genHandleFailMessage(String.format("Event:%s not handle", handleEventExtraData.getUniqueId()));
                break;
        }
        return result;
    }

    private HandleResult handEventFeeRate(String[] topics, String data, HandleEventExtraData handleEventExtraData) {
        EventValues eventValues = getEventValue(
                SwapV1FactoryEvent.EVENT_NAME_FEE_RATE,
                SwapV1FactoryEvent.EVENT_NAME_FEE_RATE_BODY,
                topics,
                data,
                handleEventExtraData.getUniqueId());
        if (ObjectUtil.isNull(eventValues)) {
            return HandleResult.genHandleFailMessage(String.format("Contract%s, type:%s decode handEventFeeRate fail!, unique id :%s",
                    address, type, handleEventExtraData.getUniqueId()));
        }
        SwapFactoryV1Data factoryV1Data = this.getVarFactoryV1Data();
        BigInteger feeRate = (BigInteger) eventValues.getNonIndexedValues().get(0).getValue();
        factoryV1Data.setFeeToRate(feeRate.longValue());
        this.isDirty = true;
        return HandleResult.genHandleSuccess();
    }

    private HandleResult handEventFeeTo(String[] topics, String data, HandleEventExtraData handleEventExtraData) {
        EventValues eventValues = getEventValue(
                SwapV1FactoryEvent.EVENT_NAME_FEE_TO,
                SwapV1FactoryEvent.EVENT_NAME_FEE_TO_BODY,
                topics,
                data,
                handleEventExtraData.getUniqueId());
        if (ObjectUtil.isNull(eventValues)) {
            return HandleResult.genHandleFailMessage(String.format("Contract%s, type:%s decode handEventFeeTo fail!, unique id :%s",
                    address, type, handleEventExtraData.getUniqueId()));
        }
        SwapFactoryV1Data factoryV1Data = this.getVarFactoryV1Data();
        String feeAddress = WalletUtil.ethAddressToTron((String) eventValues.getNonIndexedValues().get(0).getValue());
        factoryV1Data.setFeeAddress(feeAddress);
        this.isDirty = true;
        return HandleResult.genHandleSuccess();
    }

    private HandleResult handEventNewExchange(String[] _topics, String _data) {
        isAddExchangeContracts = false;
        SwapFactoryV1Data factoryV1Data = this.getVarFactoryV1Data();
        factoryV1Data.setAddExchangeContracts(false);
        factoryV1Data.setTokenCount(factoryV1Data.getTokenCount() + 1);
        this.isDirty = true;
        return HandleResult.genHandleSuccess();
    }
}
