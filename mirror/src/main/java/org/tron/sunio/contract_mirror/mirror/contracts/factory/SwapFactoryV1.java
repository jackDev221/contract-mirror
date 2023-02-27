package org.tron.sunio.contract_mirror.mirror.contracts.factory;

import cn.hutool.core.util.ObjectUtil;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.tron.sunio.contract_mirror.event_decode.events.SwapV1Event;
import org.tron.sunio.contract_mirror.event_decode.events.SwapV1FactoryEvent;
import org.tron.sunio.contract_mirror.mirror.chainHelper.IChainHelper;
import org.tron.sunio.contract_mirror.mirror.contracts.BaseContract;
import org.tron.sunio.contract_mirror.mirror.contracts.IContractsHelper;
import org.tron.sunio.contract_mirror.mirror.contracts.impl.SwapV1;
import org.tron.sunio.contract_mirror.mirror.dao.SwapFactoryV1Data;
import org.tron.sunio.contract_mirror.mirror.enums.ContractType;
import org.tron.sunio.contract_mirror.mirror.pool.CMPool;
import org.tron.sunio.contract_mirror.mirror.pool.process.out.BaseProcessOut;
import org.tron.sunio.contract_mirror.mirror.pool.process.out.SwapV1FactoryExOut;
import org.tron.sunio.contract_mirror.mirror.tools.CallContractUtil;
import org.tron.sunio.tronsdk.WalletUtil;
import org.web3j.abi.EventValues;
import org.web3j.abi.FunctionReturnDecoder;
import org.web3j.abi.TypeReference;
import org.web3j.abi.Utils;
import org.web3j.abi.datatypes.Address;
import org.web3j.abi.datatypes.Type;
import org.web3j.abi.datatypes.generated.Uint256;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.tron.sunio.contract_mirror.mirror.consts.ContractMirrorConst.EMPTY_ADDRESS;
import static org.tron.sunio.contract_mirror.mirror.consts.ContractMirrorConst.METHOD_FEE_TO;
import static org.tron.sunio.contract_mirror.mirror.consts.ContractMirrorConst.METHOD_FEE_TO_RATE;
import static org.tron.sunio.contract_mirror.mirror.consts.ContractMirrorConst.METHOD_GET_EXCHANGE;
import static org.tron.sunio.contract_mirror.mirror.consts.ContractMirrorConst.METHOD_GET_TOKEN;
import static org.tron.sunio.contract_mirror.mirror.consts.ContractMirrorConst.METHOD_GET_TOKEN_WITH_ID;
import static org.tron.sunio.contract_mirror.mirror.consts.ContractMirrorConst.METHOD_TOKEN_COUNT;

@Slf4j
public class SwapFactoryV1 extends BaseFactory {
    private Map<String, String> v1SigMap;
    @Setter
    private SwapFactoryV1Data swapFactoryV1Data;

    public SwapFactoryV1(String address, IChainHelper iChainHelper, IContractsHelper iContractsHelper, final Map<String, String> sigMap) {
        super(address, ContractType.SWAP_FACTORY_V1, iChainHelper, iContractsHelper, sigMap);
        v1SigMap = SwapV1Event.getSigMap();
    }

    private SwapFactoryV1Data getVarFactoryV1Data() {
        if (ObjectUtil.isNull(swapFactoryV1Data)) {
            swapFactoryV1Data = new SwapFactoryV1Data();
            swapFactoryV1Data.setReady(false);
            swapFactoryV1Data.setUsing(true);
            swapFactoryV1Data.setAddress(this.address);
            swapFactoryV1Data.setType(this.type);
        }
        return swapFactoryV1Data;
    }

    public SwapFactoryV1Data getSwapFactoryV1Data() {
        return getVarFactoryV1Data().copySelf();
    }

    @Override
    public boolean initDataFromChain1() {
        SwapFactoryV1Data swapFactoryV1Data = getVarFactoryV1Data();
        String feeTo = CallContractUtil.getTronAddress(iChainHelper, EMPTY_ADDRESS, address, "feeTo");
        swapFactoryV1Data.setFeeTo(feeTo);
        long feeToRate = CallContractUtil.getU256(iChainHelper, EMPTY_ADDRESS, address, "feeToRate").longValue();
        swapFactoryV1Data.setFeeToRate(feeToRate);
        long tokenCount = CallContractUtil.getU256(iChainHelper, EMPTY_ADDRESS, address, "tokenCount").longValue();
        swapFactoryV1Data.setTokenCount(tokenCount);
        isDirty = true;
        return true;
    }

    @Override
    public BaseFactory getBaseContract() {
        return this;
    }

    @Override
    public List<BaseContract> getListContracts(CMPool cmPool) {
        log.info("SwapFactoryV1: getListContracts");
        List<BaseContract> result = new ArrayList<>();
        SwapFactoryV1Data v1Data = this.getVarFactoryV1Data();
        long totalTokens = v1Data.getTokenCount();
//        totalTokens = 3;
        List<BaseProcessOut> outs = this.getListContractsBase(cmPool, (int) totalTokens);
        for (BaseProcessOut out : outs) {
            SwapV1FactoryExOut swapV1FactoryExOut = (SwapV1FactoryExOut) out;
            String v1Address = swapV1FactoryExOut.getAddress();
            String tokenAddress = swapV1FactoryExOut.getTokenAddress();
            v1Data.getTokenToExchangeMap().put(tokenAddress, v1Address);
            v1Data.getExchangeToTokenMap().put(v1Address, tokenAddress);
            v1Data.getIdTokenMap().put(out.getId(), tokenAddress);
            if (v1Address.equals(EMPTY_ADDRESS) || tokenAddress.equals(EMPTY_ADDRESS)) {
                continue;
            }
            SwapV1 swapV1 = new SwapV1(this.address, v1Address,
                    this.iChainHelper, this.getIContractsHelper(), tokenAddress, v1SigMap);

            result.add(swapV1);
        }
        return result;
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

    @Override
    public <T> T getStatus() {
        return (T) getVarFactoryV1Data();
    }

    @Override
    public <T> T handleSpecialRequest(String method, String params) throws Exception {
        switch (method) {
            case METHOD_FEE_TO:
                return (T) this.getVarFactoryV1Data().getFeeTo();
            case METHOD_FEE_TO_RATE:
                return (T) (Long) this.getVarFactoryV1Data().getFeeToRate();
            case METHOD_TOKEN_COUNT:
                return (T) (Long) this.getVarFactoryV1Data().getTokenCount();
            case METHOD_GET_TOKEN_WITH_ID:
                return handleCallGetTokenWithID(params);
            case METHOD_GET_EXCHANGE:
                return handleCallGetExchangeOrToken(params, true);
            case METHOD_GET_TOKEN:
                return handleCallGetExchangeOrToken(params, false);

        }
        return null;
    }

    public <T> T handleCallGetTokenWithID(String params) {
        List<TypeReference<?>> outputParameters = List.of(new TypeReference<Uint256>() {
        });
        List<Type> res = FunctionReturnDecoder.decode(params, Utils.convert(outputParameters));
        if (res.size() == 0) {
            throw new RuntimeException("Decode failed");
        }
        int id = ((BigInteger) res.get(0).getValue()).intValue();
        return (T) this.getVarFactoryV1Data().getIdTokenMap().getOrDefault(id, "");
    }

    public <T> T handleCallGetExchangeOrToken(String params, boolean outExchange) {
        List<TypeReference<?>> outputParameters = List.of(new TypeReference<Address>() {
        });
        List<Type> res = FunctionReturnDecoder.decode(params, Utils.convert(outputParameters));
        if (res.size() == 0) {
            throw new RuntimeException("Decode failed");
        }
        String address = WalletUtil.hexStringToTron((String) res.get(0).getValue());
        if (outExchange) {
            return (T) this.getVarFactoryV1Data().getTokenToExchangeMap().getOrDefault(address, "");
        }
        return (T) this.getVarFactoryV1Data().getExchangeToTokenMap().getOrDefault(address, "");
    }

    private HandleResult handEventFeeRate(String[] topics, String data, HandleEventExtraData handleEventExtraData) {
        log.info("SwapFactoryV1:{}, handEventFeeRate, topics:{} data:{} ", address, topics, data);
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
        log.info("SwapFactoryV1:{}, handEventFeeTo, topics:{} data:{} ", address, topics, data);
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
        factoryV1Data.setFeeTo(feeAddress);
        this.isDirty = true;
        return HandleResult.genHandleSuccess();
    }

    private HandleResult handEventNewExchange(String[] topics, String data) {
        log.info("SwapFactoryV1:{}, handEventNewExchange, topics:{} data:{} ", address, topics, data);
        isAddExchangeContracts = false;
        SwapFactoryV1Data factoryV1Data = this.getVarFactoryV1Data();
        factoryV1Data.setAddExchangeContracts(false);
        factoryV1Data.setTokenCount(factoryV1Data.getTokenCount() + 1);
        this.isDirty = true;
        return HandleResult.genHandleSuccess();
    }
}
