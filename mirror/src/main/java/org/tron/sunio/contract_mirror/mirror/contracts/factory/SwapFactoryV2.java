package org.tron.sunio.contract_mirror.mirror.contracts.factory;

import cn.hutool.core.util.ObjectUtil;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.tron.sunio.contract_mirror.event_decode.events.SwapV2PairEvent;
import org.tron.sunio.contract_mirror.mirror.chainHelper.IChainHelper;
import org.tron.sunio.contract_mirror.mirror.contracts.BaseContract;
import org.tron.sunio.contract_mirror.mirror.contracts.IContractsHelper;
import org.tron.sunio.contract_mirror.mirror.contracts.impl.SwapV2Pair;
import org.tron.sunio.contract_mirror.mirror.dao.SwapFactoryV2Data;
import org.tron.sunio.contract_mirror.mirror.enums.ContractType;
import org.tron.sunio.contract_mirror.mirror.pool.CMPool;
import org.tron.sunio.contract_mirror.mirror.pool.process.out.BaseProcessOut;
import org.tron.sunio.contract_mirror.mirror.pool.process.out.SwapV2FactoryExOut;
import org.tron.sunio.contract_mirror.mirror.tools.CallContractUtil;
import org.tron.sunio.tronsdk.WalletUtil;
import org.web3j.abi.EventValues;
import org.web3j.abi.FunctionReturnDecoder;
import org.web3j.abi.TypeReference;
import org.web3j.abi.Utils;
import org.web3j.abi.datatypes.Type;
import org.web3j.abi.datatypes.generated.Uint256;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.tron.sunio.contract_mirror.event_decode.events.SwapV2FactoryEvent.EVENT_NAME_NEW_PAIR_CREATED_BODY;
import static org.tron.sunio.contract_mirror.event_decode.events.SwapV2FactoryEvent.EVENT_NAME_PAIR_CREATED;
import static org.tron.sunio.contract_mirror.mirror.consts.ContractMirrorConst.EMPTY_ADDRESS;
import static org.tron.sunio.contract_mirror.mirror.consts.ContractMirrorConst.METHOD_ALL_PAIRS;
import static org.tron.sunio.contract_mirror.mirror.consts.ContractMirrorConst.METHOD_ALL_PAIRS_LENGTH;
import static org.tron.sunio.contract_mirror.mirror.consts.ContractMirrorConst.METHOD_FEE_TO;
import static org.tron.sunio.contract_mirror.mirror.consts.ContractMirrorConst.METHOD_FEE_TO_SETTER;
import static org.tron.sunio.contract_mirror.mirror.consts.ContractMirrorConst.METHOD_GET_PAIR;
import static org.tron.sunio.contract_mirror.mirror.consts.ContractMirrorConst.V2_FACTORY;

@Slf4j
public class SwapFactoryV2 extends BaseFactory {
    private final Map<String, String> v2PairSigMap;
    @Setter
    private SwapFactoryV2Data swapFactoryV2Data;

    public SwapFactoryV2(String address, IChainHelper iChainHelper, IContractsHelper iContractsHelper,
                         Map<String, String> sigMap) {
        super(address, ContractType.SWAP_FACTORY_V2, V2_FACTORY, iChainHelper, iContractsHelper, sigMap);
        v2PairSigMap = SwapV2PairEvent.getSigMap();
    }

    private SwapFactoryV2Data getVarFactoryV2Data() {
        if (ObjectUtil.isNull(swapFactoryV2Data)) {
            swapFactoryV2Data = new SwapFactoryV2Data();
            swapFactoryV2Data.setStateInfo(stateInfo);
            swapFactoryV2Data.setAddress(this.address);
            swapFactoryV2Data.setType(this.type);
            swapFactoryV2Data.setVersion(version);
        }
        return swapFactoryV2Data;
    }

    public SwapFactoryV2Data getSwapFactoryV2Data() {
        return getVarFactoryV2Data().copySelf();
    }

    @Override
    public boolean initDataFromChain1() {
        SwapFactoryV2Data factoryV2Data = this.getVarFactoryV2Data();
        String feeTo = CallContractUtil.getTronAddress(iChainHelper, EMPTY_ADDRESS, address, "feeTo");
        factoryV2Data.setFeeTo(feeTo);
        String feeToSetter = CallContractUtil.getTronAddress(iChainHelper, EMPTY_ADDRESS, address, "feeToSetter");
        factoryV2Data.setFeeToSetter(feeToSetter);
        long feeToRate = CallContractUtil.getU256(iChainHelper, EMPTY_ADDRESS, address, "feeToRate").longValue();
        factoryV2Data.setPairCount(feeToRate);
        long pairCount = CallContractUtil.getU256(iChainHelper, EMPTY_ADDRESS, address, "allPairsLength").longValue();
        factoryV2Data.setPairCount(pairCount);
        stateInfo.dirty = true;
        return true;
    }

    @Override
    public BaseFactory getBaseContract() {
        return this;
    }

    @Override
    public String getVersion() {
        return version;
    }

    @Override
    public List<BaseContract> getListContracts(CMPool cmPool) {
        log.info("SwapFactoryV2: getListContracts");
        List<BaseContract> result = newSubContracts;
        newSubContracts = new ArrayList<>();
        this.hasNewContract = false;
        if (stateInfo.addExchangeContracts) {
            return result;
        }
        long pairCount = this.getVarFactoryV2Data().getPairCount();
//        pairCount = 3;
        List<BaseProcessOut> outs = this.getListContractsBase(cmPool, (int) pairCount);
        for (BaseProcessOut out : outs) {
            SwapV2FactoryExOut swapV2FactoryExOut = (SwapV2FactoryExOut) out;
            updateTokenToPairs(out.getAddress(), swapV2FactoryExOut.getToken0(), swapV2FactoryExOut.getToken1());
            updateTokenToPairs(out.getAddress(), swapV2FactoryExOut.getToken1(), swapV2FactoryExOut.getToken0());
            this.getVarFactoryV2Data().getPairsMap().put(out.getId(), out.getAddress());
            String pairAddress = out.getAddress();
            if (pairAddress.equals(EMPTY_ADDRESS)) {
                continue;
            }
            SwapV2Pair swapV2Pair = new SwapV2Pair(
                    pairAddress,
                    this.address,
                    this.iChainHelper,
                    this.getIContractsHelper(),
                    v2PairSigMap

            );

            result.add(swapV2Pair);
        }
        return result;
    }

    private void updateTokenToPairs(String pair, String token0, String token1) {
        SwapFactoryV2Data v2Data = this.getVarFactoryV2Data();
        Map<String, Map<String, String>> tokensToPairMaps = v2Data.getTokensToPairMaps();
        Map<String, String> itemMap = tokensToPairMaps.getOrDefault(token0, new HashMap<>());
        itemMap.put(token1, pair);
        tokensToPairMaps.put(token0, itemMap);

    }

    @Override
    protected void saveUpdateToCache() {
    }

    @Override
    protected HandleResult handleEvent1(String eventName, String[] topics, String data, HandleEventExtraData handleEventExtraData) {
        HandleResult result;
        switch (eventName) {
            case EVENT_NAME_PAIR_CREATED:
                result = handleCreatePair(topics, data, handleEventExtraData);
                break;
            default:
                log.warn("Contract:{} type:{} event:{} not handle", address, type, topics[0]);
                result = HandleResult.genHandleUselessMessage(String.format("Event:%s not handle", handleEventExtraData.getUniqueId()));
                break;
        }
        return result;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getStatus() {
        return (T) getVarFactoryV2Data();
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T handleSpecialRequest(String method, String params) throws Exception {
        switch (method) {
            case METHOD_FEE_TO:
                return (T) this.getVarFactoryV2Data().getFeeTo();
            case METHOD_FEE_TO_SETTER:
                return (T) this.getVarFactoryV2Data().getFeeToSetter();
            case METHOD_ALL_PAIRS_LENGTH:
                return (T) (Long) this.getVarFactoryV2Data().getPairCount();
            case METHOD_ALL_PAIRS:
                return handleCallAllPairs(params);
            case METHOD_GET_PAIR:
                return handleCallGetPairs(params);
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    public <T> T handleCallAllPairs(String params) throws Exception {
        List<TypeReference<?>> outputParameters = List.of(new TypeReference<Uint256>() {
        });
        List<Type> res = FunctionReturnDecoder.decode(params, Utils.convert(outputParameters));
        if (res.size() == 0) {
            throw new RuntimeException("Decode failed");
        }
        int id = ((BigInteger) res.get(0).getValue()).intValue();
        return (T) this.getVarFactoryV2Data().getPairsMap().getOrDefault(id, "");
    }

    @SuppressWarnings("unchecked")
    public <T> T handleCallGetPairs(String params) throws Exception {
        List<TypeReference<?>> outputParameters = new ArrayList<>();
        outputParameters.add(new TypeReference<Uint256>() {
        });
        outputParameters.add(new TypeReference<Uint256>() {
        });
        List<Type> res = FunctionReturnDecoder.decode(params, Utils.convert(outputParameters));
        if (res.size() < 2) {
            throw new RuntimeException("Decode failed");
        }
        String token0 = WalletUtil.hexStringToTron((String) res.get(0).getValue());
        String token1 = WalletUtil.hexStringToTron((String) res.get(1).getValue());
        var tokensToPairMaps = this.getVarFactoryV2Data().getTokensToPairMaps();
        if (tokensToPairMaps.containsKey(token0)) {
            return (T) tokensToPairMaps.get(token0).getOrDefault(token1, "");
        } else {
            throw new RuntimeException(String.format("Token0 input %s not exist", token0));
        }
    }

    private HandleResult handleCreatePair(String[] topics, String data, HandleEventExtraData handleEventExtraData) {
        log.info("SwapFactoryV2:{}, handleCreatePair, topics:{} data:{} ", address, topics, data);
        EventValues eventValues = getEventValue(
                EVENT_NAME_PAIR_CREATED,
                EVENT_NAME_NEW_PAIR_CREATED_BODY,
                topics,
                data,
                handleEventExtraData.getUniqueId());
        if (ObjectUtil.isNull(eventValues)) {
            return HandleResult.genHandleFailMessage(String.format("Contract%s, type:%s decode handleCreatePair fail!, unique id :%s",
                    address, type, handleEventExtraData.getUniqueId()));
        }
        String token0 = WalletUtil.hexStringToTron((String) eventValues.getIndexedValues().get(0).getValue());
        String token1 = WalletUtil.hexStringToTron((String) eventValues.getIndexedValues().get(1).getValue());
        String pairAddress = WalletUtil.hexStringToTron((String) eventValues.getNonIndexedValues().get(0).getValue());
        SwapV2Pair swapV2Pair = new SwapV2Pair(
                pairAddress,
                this.address,
                this.iChainHelper,
                this.getIContractsHelper(),
                v2PairSigMap

        );

        newSubContracts.add(swapV2Pair);
        updateTokenToPairs(pairAddress, token0, token1);
        updateTokenToPairs(pairAddress, token0, token1);
        SwapFactoryV2Data factoryV2Data = this.getVarFactoryV2Data();
        factoryV2Data.setPairCount(factoryV2Data.getPairCount() + 1);
        hasNewContract = true;
        stateInfo.dirty = true;
        return HandleResult.genHandleSuccess();
    }
}
