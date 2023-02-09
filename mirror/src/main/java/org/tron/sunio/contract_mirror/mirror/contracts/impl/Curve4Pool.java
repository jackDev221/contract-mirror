package org.tron.sunio.contract_mirror.mirror.contracts.impl;

import cn.hutool.core.util.ObjectUtil;
import lombok.extern.slf4j.Slf4j;
import org.tron.sunio.contract_mirror.event_decode.events.Curve2PoolEvent;
import org.tron.sunio.contract_mirror.mirror.chainHelper.IChainHelper;
import org.tron.sunio.contract_mirror.mirror.chainHelper.TriggerContractInfo;
import org.tron.sunio.contract_mirror.mirror.consts.ContractMirrorConst;
import org.tron.sunio.contract_mirror.mirror.contracts.BaseContract;
import org.tron.sunio.contract_mirror.mirror.contracts.IContractsHelper;
import org.tron.sunio.contract_mirror.mirror.dao.Curve4PoolData;
import org.tron.sunio.contract_mirror.mirror.enums.ContractType;
import org.tron.sunio.tronsdk.WalletUtil;
import org.web3j.abi.EventValues;
import org.web3j.abi.FunctionEncoder;
import org.web3j.abi.TypeReference;
import org.web3j.abi.datatypes.Address;
import org.web3j.abi.datatypes.StaticArray;
import org.web3j.abi.datatypes.Type;
import org.web3j.abi.datatypes.generated.StaticArray2;
import org.web3j.abi.datatypes.generated.Uint256;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.tron.sunio.contract_mirror.event_decode.events.Curve4PoolEvent.EVENT_NAME_ADD_LIQUIDITY_BODY;
import static org.tron.sunio.contract_mirror.event_decode.events.Curve4PoolEvent.EVENT_NAME_COMMIT_NEW_ADMIN_BODY;
import static org.tron.sunio.contract_mirror.event_decode.events.Curve4PoolEvent.EVENT_NAME_COMMIT_NEW_FEE_BODY;
import static org.tron.sunio.contract_mirror.event_decode.events.Curve4PoolEvent.EVENT_NAME_NEW_ADMIN_BODY;
import static org.tron.sunio.contract_mirror.event_decode.events.Curve4PoolEvent.EVENT_NAME_NEW_FEE_BODY;
import static org.tron.sunio.contract_mirror.event_decode.events.Curve4PoolEvent.EVENT_NAME_RAMP_A_BODY;
import static org.tron.sunio.contract_mirror.event_decode.events.Curve4PoolEvent.EVENT_NAME_REMOVE_LIQUIDITY_BODY;
import static org.tron.sunio.contract_mirror.event_decode.events.Curve4PoolEvent.EVENT_NAME_REMOVE_LIQUIDITY_IM_BALANCE_BODY;
import static org.tron.sunio.contract_mirror.event_decode.events.Curve4PoolEvent.EVENT_NAME_STOP_RAMP_A_BODY;
import static org.tron.sunio.contract_mirror.event_decode.events.Curve4PoolEvent.EVENT_NAME_ADD_LIQUIDITY;
import static org.tron.sunio.contract_mirror.event_decode.events.Curve4PoolEvent.EVENT_NAME_COMMIT_NEW_ADMIN;
import static org.tron.sunio.contract_mirror.event_decode.events.Curve4PoolEvent.EVENT_NAME_COMMIT_NEW_FEE;
import static org.tron.sunio.contract_mirror.event_decode.events.Curve4PoolEvent.EVENT_NAME_NEW_ADMIN;
import static org.tron.sunio.contract_mirror.event_decode.events.Curve4PoolEvent.EVENT_NAME_NEW_FEE;
import static org.tron.sunio.contract_mirror.event_decode.events.Curve4PoolEvent.EVENT_NAME_NEW_FEE_CONVERTER;
import static org.tron.sunio.contract_mirror.event_decode.events.Curve4PoolEvent.EVENT_NAME_RAMP_A;
import static org.tron.sunio.contract_mirror.event_decode.events.Curve4PoolEvent.EVENT_NAME_REMOVE_LIQUIDITY;
import static org.tron.sunio.contract_mirror.event_decode.events.Curve4PoolEvent.EVENT_NAME_REMOVE_LIQUIDITY_IM_BALANCE;
import static org.tron.sunio.contract_mirror.event_decode.events.Curve4PoolEvent.EVENT_NAME_REMOVE_LIQUIDITY_ONE;
import static org.tron.sunio.contract_mirror.event_decode.events.Curve4PoolEvent.EVENT_NAME_STOP_RAMP_A;
import static org.tron.sunio.contract_mirror.event_decode.events.Curve4PoolEvent.EVENT_NAME_TOKEN_EXCHANGE;
import static org.tron.sunio.contract_mirror.event_decode.events.Curve4PoolEvent.EVENT_NAME_TOKEN_EXCHANGE_BODY;
import static org.tron.sunio.contract_mirror.event_decode.events.Curve4PoolEvent.EVENT_NAME_TOKEN_EXCHANGE_UNDERLING;

@Slf4j
public class Curve4Pool extends BaseContract {
    private static final int N_COINS = 2;
    private static final int MAX_COIN = N_COINS - 1;
    private static final int N_BASE_COINS = 3;
    private static final long BASE_CACHE_EXPIRES = 10 * 60;
    private static final BigInteger PRECISION = BigInteger.TEN.pow(18);
    private static final BigInteger FEE_DENOMINATOR = BigInteger.TEN.pow(10);
    private Curve4PoolData curve4PoolData;

    public Curve4Pool(String address, IChainHelper iChainHelper, IContractsHelper iContractsHelper, Map<String, String> sigMap) {
        super(address, ContractType.CONTRACT_CURVE_4POOL, iChainHelper, iContractsHelper, sigMap);
    }

    private Curve4PoolData getVarCurve4PoolData() {
        if (ObjectUtil.isNull(curve4PoolData)) {
            curve4PoolData = new Curve4PoolData();
            curve4PoolData.setAddress(address);
            curve4PoolData.setType(type);
            curve4PoolData.setUsing(true);
            curve4PoolData.setReady(false);
            curve4PoolData.setAddExchangeContracts(false);
        }
        return curve4PoolData;
    }

    private void updateCoinsAndBalance(Curve4PoolData curve4PoolData) {
        for (int i = 0; i < N_BASE_COINS; i++) {
            // update base_coins
            TriggerContractInfo triggerContractInfo = new TriggerContractInfo(ContractMirrorConst.EMPTY_ADDRESS, address, "base_coins",
                    List.of(new Uint256(i)), List.of(new TypeReference<Uint256>() {
            }));
            List<Type> results = this.iChainHelper.triggerConstantContract(triggerContractInfo);
            if (results.size() == 0) {
                log.error("Get contract:{} type:{} , function:{} result len is zero", this.address, this.type, "base_coins");
            } else {
                curve4PoolData.updateBaseCoins(i, WalletUtil.hexStringToTron((String) results.get(0).getValue()));
            }

            if (i >= N_COINS) {
                continue;
            }

            // update coins string
            triggerContractInfo = new TriggerContractInfo(ContractMirrorConst.EMPTY_ADDRESS, address,
                    "coins", List.of(new Uint256(i)), List.of(new TypeReference<Address>() {
            })
            );
            results = this.iChainHelper.triggerConstantContract(triggerContractInfo);
            if (results.size() == 0) {
                log.error("Get contract:{} type:{} , function:{} result len is zero", this.address, this.type, "coins");
            } else {
                curve4PoolData.updateCoins(i, WalletUtil.hexStringToTron((String) results.get(0).getValue()));
            }


            // update coins balance
            triggerContractInfo = new TriggerContractInfo(ContractMirrorConst.EMPTY_ADDRESS, address, "balances",
                    List.of(new Uint256(i)), List.of(new TypeReference<Uint256>() {
            }));
            results = this.iChainHelper.triggerConstantContract(triggerContractInfo);
            if (results.size() == 0) {
                log.error("Get contract:{} type:{} , function:{} result len is zero", this.address, this.type, "balances");
            } else {
                curve4PoolData.updateBalances(i, (BigInteger) results.get(0).getValue());
            }
        }
    }

    private void updateSupply(Curve4PoolData curve4PoolData, String tokenAddress) {
        TriggerContractInfo triggerContractInfo = new TriggerContractInfo(ContractMirrorConst.EMPTY_ADDRESS, tokenAddress,
                "totalSupply", Collections.emptyList(), List.of(new TypeReference<Uint256>() {
        }));
        List<Type> results = this.iChainHelper.triggerConstantContract(triggerContractInfo);
        if (results.size() == 0) {
            log.error("Get contract:{} type:{} , token: {} function: totalSupply() result len is zero", this.address, this.type, tokenAddress);
        } else {
            curve4PoolData.setTotalSupply((BigInteger) results.get(0).getValue());
        }
    }

    @Override
    public boolean initDataFromChain1() {
        Curve4PoolData curve4PoolData = this.getVarCurve4PoolData();
        updateCoinsAndBalance(curve4PoolData);
        String token = WalletUtil.ethAddressToTron(callContractAddress(ContractMirrorConst.EMPTY_ADDRESS, "lp_token").toString());
        curve4PoolData.setLpToken(token);
        updateSupply(curve4PoolData, token);

        String basePool = WalletUtil.ethAddressToTron(callContractAddress(ContractMirrorConst.EMPTY_ADDRESS, "base_pool").toString());
        curve4PoolData.setBasePool(basePool);
        String baseLp = WalletUtil.ethAddressToTron(callContractAddress(ContractMirrorConst.EMPTY_ADDRESS, "base_lp").toString());
        curve4PoolData.setBaseLp(baseLp);
        BigInteger baseVirtualPrice = callContractU256(ContractMirrorConst.EMPTY_ADDRESS, "base_virtual_price");
        curve4PoolData.setBaseVirtualPrice(baseVirtualPrice);
        BigInteger baseCacheUpdated = callContractU256(ContractMirrorConst.EMPTY_ADDRESS, "base_cache_updated");
        curve4PoolData.setBaseCacheUpdated(baseCacheUpdated.longValue());
        BigInteger fee = callContractU256(ContractMirrorConst.EMPTY_ADDRESS, "fee");
        curve4PoolData.setFee(fee);
        BigInteger futureFee = callContractU256(ContractMirrorConst.EMPTY_ADDRESS, "future_fee");
        curve4PoolData.setFutureFee(futureFee);
        BigInteger adminFee = callContractU256(ContractMirrorConst.EMPTY_ADDRESS, "admin_fee");
        curve4PoolData.setAdminFee(adminFee);
        BigInteger futureAdminFee = callContractU256(ContractMirrorConst.EMPTY_ADDRESS, "future_admin_fee");
        curve4PoolData.setFutureAdminFee(futureAdminFee);
        BigInteger adminActionsDeadline = callContractU256(ContractMirrorConst.EMPTY_ADDRESS, "admin_actions_deadline");
        curve4PoolData.setAdminActionsDeadline(adminActionsDeadline);
        String feeConverter = WalletUtil.ethAddressToTron(callContractAddress(ContractMirrorConst.EMPTY_ADDRESS, "fee_converter").toString());
        curve4PoolData.setFeeConverter(feeConverter);
        BigInteger initialA = callContractU256(ContractMirrorConst.EMPTY_ADDRESS, "initial_A");
        curve4PoolData.setInitialA(initialA);
        BigInteger initialATime = callContractU256(ContractMirrorConst.EMPTY_ADDRESS, "initial_A_time");
        curve4PoolData.setInitialATime(initialATime);
        BigInteger futureA = callContractU256(ContractMirrorConst.EMPTY_ADDRESS, "future_A");
        curve4PoolData.setFutureA(futureA);
        BigInteger futureATime = callContractU256(ContractMirrorConst.EMPTY_ADDRESS, "future_A_time");
        curve4PoolData.setFutureATime(futureATime);
        String owner = WalletUtil.ethAddressToTron(callContractAddress(ContractMirrorConst.EMPTY_ADDRESS, "owner").toString());
        curve4PoolData.setOwner(owner);
        String futureOwner = WalletUtil.ethAddressToTron(callContractAddress(ContractMirrorConst.EMPTY_ADDRESS, "future_owner").toString());
        curve4PoolData.setFutureOwner(futureOwner);
        BigInteger transferOwnershipDeadline = callContractU256(ContractMirrorConst.EMPTY_ADDRESS, "transfer_ownership_deadline");
        curve4PoolData.setTransferOwnershipDeadline(transferOwnershipDeadline);
        isDirty = true;
        return true;
    }

    @Override
    public void updateBaseInfo(boolean isUsing, boolean isReady, boolean isAddExchangeContracts) {
        Curve4PoolData curve4PoolData = this.getVarCurve4PoolData();
        curve4PoolData.setUsing(isUsing);
        curve4PoolData.setReady(isReady);
        curve4PoolData.setAddExchangeContracts(isAddExchangeContracts);
        isDirty = true;
    }

    @Override
    protected void saveUpdateToCache() {
    }

    @Override
    protected HandleResult handleEvent1(String eventName, String[] topics, String data, HandleEventExtraData handleEventExtraData) {
        HandleResult result;
        switch (eventName) {
            case EVENT_NAME_TOKEN_EXCHANGE_UNDERLING:
                result = handleEventTokenExchangeUnderlying(topics, data, handleEventExtraData);
                break;
            case EVENT_NAME_TOKEN_EXCHANGE:
                result = handleEventTokenExchange(topics, data, handleEventExtraData);
                break;
            case EVENT_NAME_ADD_LIQUIDITY:
                result = handleEventAddLiquidity(topics, data, handleEventExtraData);
                break;
            case EVENT_NAME_REMOVE_LIQUIDITY:
                result = handleEventRemoveLiquidity(topics, data, handleEventExtraData);
                break;
            case EVENT_NAME_REMOVE_LIQUIDITY_ONE:
                result = handleEventRemoveLiquidityOne(topics, data, handleEventExtraData);
                break;
            case EVENT_NAME_REMOVE_LIQUIDITY_IM_BALANCE:
                result = handleEventRemoveLiquidityImbalance(topics, data, handleEventExtraData);
                break;
            case EVENT_NAME_COMMIT_NEW_ADMIN:
                result = handleEventCommitNewAdmin(topics, data, handleEventExtraData);
                break;
            case EVENT_NAME_NEW_ADMIN:
                result = handleEventNewAdmin(topics, data, handleEventExtraData);
                break;
            case EVENT_NAME_NEW_FEE_CONVERTER:
                result = handleEventNewFeeConverter(topics, data, handleEventExtraData);
                break;
            case EVENT_NAME_COMMIT_NEW_FEE:
                result = handleEventCommitNewFee(topics, data, handleEventExtraData);
                break;
            case EVENT_NAME_NEW_FEE:
                result = handleEventNewFee(topics, data, handleEventExtraData);
                break;
            case EVENT_NAME_RAMP_A:
                result = handleEventRampA(topics, data, handleEventExtraData);
                break;
            case EVENT_NAME_STOP_RAMP_A:
                result = handleEventStopRampA(topics, data, handleEventExtraData);
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
        return (T) getVarCurve4PoolData();
    }

    @Override
    public <T> T handleSpecialRequest(String method, String params) throws Exception {
        return null;
    }

    private HandleResult handleEventTokenExchangeUnderlying(String[] topics, String data, HandleEventExtraData handleEventExtraData) {
        log.info("{}:{} handleEventTokenExchange:{} not implements!!", address, type, handleEventExtraData.getUniqueId());
//        EventValues eventValues = getEventValue(EVENT_NAME_TOKEN_EXCHANGE_UNDERLING, EVENT_NAME_TOKEN_EXCHANGE_UNDERLING_BODY, topics, data,
//                handleEventExtraData.getUniqueId());
//        if (ObjectUtil.isNull(eventValues)) {
//            return HandleResult.genHandleFailMessage(String.format("Contract%s, type:%s decode handleEventTokenExchange fail!, unique id :%s",
//                    address, type, handleEventExtraData.getUniqueId()));
//        }
//        Curve4PoolData v4Data = this.getVarCurve4PoolData();
//        int i = ((BigInteger) eventValues.getNonIndexedValues().get(0).getValue()).intValue();
//        int j = ((BigInteger) eventValues.getNonIndexedValues().get(2).getValue()).intValue();
//
//        int baseI = i - MAX_COIN;
//        int baseJ = j - MAX_COIN;
//        int metaI = MAX_COIN;
//        int metaJ = MAX_COIN;
//        if (baseI < 0){
//            metaI = i;
//        }
//        if (baseJ < 0){
//            metaJ = j;
//        }
//        BigInteger dx = (BigInteger) eventValues.getNonIndexedValues().get(1).getValue();
//        BigInteger dxWFee = getAmountWFee(i, dx);
//        BigInteger[] rates = new BigInteger[]{BigInteger.TEN.pow(30), BigInteger.TEN.pow(18)};
//        rates[MAX_COIN] = updateVpRate(handleEventExtraData.getTimeStamp());
        return HandleResult.genHandleFailMessage("TokenExchangeUnderlying not implements!");
    }

    private HandleResult handleEventTokenExchange(String[] topics, String data, HandleEventExtraData handleEventExtraData) {
        log.info("{}:{} handleEventTokenExchange:{}", address, type, handleEventExtraData.getUniqueId());
        EventValues eventValues = getEventValue(EVENT_NAME_TOKEN_EXCHANGE, EVENT_NAME_TOKEN_EXCHANGE_BODY, topics, data,
                handleEventExtraData.getUniqueId());
        if (ObjectUtil.isNull(eventValues)) {
            return HandleResult.genHandleFailMessage(String.format("Contract%s, type:%s decode handleEventTokenExchange fail!, unique id :%s",
                    address, type, handleEventExtraData.getUniqueId()));
        }
        Curve4PoolData v4Data = this.getVarCurve4PoolData();
        int i = ((BigInteger) eventValues.getNonIndexedValues().get(0).getValue()).intValue();
        BigInteger dx = (BigInteger) eventValues.getNonIndexedValues().get(1).getValue();
        int j = ((BigInteger) eventValues.getNonIndexedValues().get(2).getValue()).intValue();
        BigInteger[] rates = new BigInteger[]{BigInteger.TEN.pow(30), BigInteger.TEN.pow(18)};
        rates[MAX_COIN] = updateVpRate(handleEventExtraData.getTimeStamp());
        BigInteger dy = (BigInteger) eventValues.getNonIndexedValues().get(3).getValue();
        BigInteger tmp = dy.multiply(rates[j]).multiply(FEE_DENOMINATOR);
        BigInteger dyOri = (tmp.divide(FEE_DENOMINATOR.subtract(v4Data.getFee()))).divide(PRECISION);
        BigInteger dyFee = dyOri.multiply(v4Data.getFee()).divide(FEE_DENOMINATOR);
        BigInteger dyAdminFee = dyFee.multiply(v4Data.getAdminFee()).divide(FEE_DENOMINATOR);
        dyAdminFee = dyAdminFee.multiply(PRECISION).divide(rates[j]);
        BigInteger dxWFee = getAmountWFee(i, dx);
        BigInteger newIBalance = v4Data.getBalances()[i].add(dxWFee);
        BigInteger newJBalance = v4Data.getBalances()[j].subtract(dy).subtract(dyAdminFee);
        v4Data.updateBalances(i, newIBalance);
        v4Data.updateBalances(j, newJBalance);
        this.isDirty = true;
        return HandleResult.genHandleSuccess();
    }

    private BigInteger getAmountWFee(int _tokenId, BigInteger dx) {
        // 查看CurvePoolV2 feeIndex 是USDT 不是特殊收费ERC20，之后需要在添加
        return dx;
    }

    private BigInteger updateVpRate(long blockTimeStamp) {
        Curve4PoolData v4Data = this.getVarCurve4PoolData();
        if (v4Data.getBaseCacheUpdated() + BASE_CACHE_EXPIRES < blockTimeStamp) {
            // TODO update vir
            v4Data.setBaseCacheUpdated(blockTimeStamp);
        }
        return v4Data.getBaseVirtualPrice();
    }

    private HandleResult handleEventAddLiquidity(String[] topics, String data, HandleEventExtraData handleEventExtraData) {
        log.info("{}:{} handleEventAddLiquidity:{}", address, type, handleEventExtraData.getUniqueId());
        EventValues eventValues = getEventValue(EVENT_NAME_ADD_LIQUIDITY, EVENT_NAME_ADD_LIQUIDITY_BODY, topics, data,
                handleEventExtraData.getUniqueId());
        if (ObjectUtil.isNull(eventValues)) {
            return HandleResult.genHandleFailMessage(String.format("Contract%s, type:%s decode handleEventAddLiquidity fail!, unique id :%s",
                    address, type, handleEventExtraData.getUniqueId()));
        }
        Curve4PoolData v4Data = this.getVarCurve4PoolData();
        StaticArray<Uint256> amounts = (StaticArray<Uint256>) eventValues.getNonIndexedValues().get(0);
        StaticArray<Uint256> fees = (StaticArray<Uint256>) eventValues.getNonIndexedValues().get(1);

        List<Uint256> amountsNew = new ArrayList<>();
        for (int i = 0; i < N_COINS; i++) {
            BigInteger amountWFee = getAmountWFee(i, (BigInteger) amounts.getValue().get(i).getValue());
            BigInteger originBalance = v4Data.getBalances()[i];
            BigInteger newBalance = originBalance.add(amountWFee);
            if (v4Data.getTotalSupply().compareTo(BigInteger.ZERO) > 0) {
                BigInteger fee = fees.getValue().get(i).getValue();
                BigInteger newFee = fee.multiply(v4Data.getAdminFee()).divide(FEE_DENOMINATOR);
                newBalance = newBalance.subtract(newFee);
            }
            v4Data.updateBalances(i, newBalance);
            amountsNew.add(new Uint256(newBalance));
        }
        BigInteger newTotalSupply = (BigInteger) eventValues.getNonIndexedValues().get(3).getValue();
        v4Data.setTotalSupply(newTotalSupply);
        this.isDirty = true;
        String newData = FunctionEncoder.encodeConstructor(
                Arrays.asList(
                        new StaticArray2(Uint256.class, amountsNew),
                        eventValues.getNonIndexedValues().get(1),
                        eventValues.getNonIndexedValues().get(2),
                        eventValues.getNonIndexedValues().get(3)
                )
        );

        return HandleResult.genHandleSuccessAndSend(topics, newData);
    }

    private HandleResult handleEventRemoveLiquidity(String[] topics, String data, HandleEventExtraData handleEventExtraData) {
        log.info("{}:{} handleEventRemoveLiquidity:{}", address, type, handleEventExtraData.getUniqueId());
        EventValues eventValues = getEventValue(EVENT_NAME_REMOVE_LIQUIDITY, EVENT_NAME_REMOVE_LIQUIDITY_BODY, topics, data,
                handleEventExtraData.getUniqueId());
        if (ObjectUtil.isNull(eventValues)) {
            return HandleResult.genHandleFailMessage(String.format("Contract%s, type:%s decode handleEventRemoveLiquidity fail!, unique id :%s",
                    address, type, handleEventExtraData.getUniqueId()));
        }
        Curve4PoolData v4Data = this.getVarCurve4PoolData();
        StaticArray<Uint256> amounts = (StaticArray<Uint256>) eventValues.getNonIndexedValues().get(0);
        List<Uint256> amountsNew = new ArrayList<>();
        for (int i = 0; i < N_COINS; i++) {
            BigInteger origin = v4Data.getBalances()[i];
            BigInteger newBalance = origin.subtract((BigInteger) amounts.getValue().get(i).getValue());
            v4Data.updateBalances(i, newBalance);
            amountsNew.add(new Uint256(newBalance));
        }
        BigInteger newTotalSupply = (BigInteger) eventValues.getNonIndexedValues().get(2).getValue();
        v4Data.setTotalSupply(newTotalSupply);
        this.isDirty = true;
        String newData = FunctionEncoder.encodeConstructor(
                Arrays.asList(
                        new StaticArray2(Uint256.class, amountsNew),
                        eventValues.getNonIndexedValues().get(1),
                        eventValues.getNonIndexedValues().get(2)
                )
        );
        return HandleResult.genHandleSuccessAndSend(topics, newData);
    }

    private HandleResult handleEventRemoveLiquidityOne(String[] topics, String data, HandleEventExtraData handleEventExtraData) {
        log.info("{}:{} handleEventRemoveLiquidityOne:{}", address, type, handleEventExtraData.getUniqueId());
        updateBaseInfo(isUsing, false, isAddExchangeContracts);
        this.isReady = false;
        this.isDirty = true;
        return HandleResult.genHandleSuccess();
    }

    private HandleResult handleEventRemoveLiquidityImbalance(String[] topics, String data, HandleEventExtraData handleEventExtraData) {
        log.info("{}:{} handleEventRemoveLiquidityImbalance:{}", address, type, handleEventExtraData.getUniqueId());
        EventValues eventValues = getEventValue(Curve2PoolEvent.EVENT_NAME_REMOVE_LIQUIDITY_IM_BALANCE, EVENT_NAME_REMOVE_LIQUIDITY_IM_BALANCE_BODY,
                topics, data, handleEventExtraData.getUniqueId());
        if (ObjectUtil.isNull(eventValues)) {
            return HandleResult.genHandleFailMessage(String.format("Contract%s, type:%s decode handleEventRemoveLiquidityImbalance fail!, unique id :%s",
                    address, type, handleEventExtraData.getUniqueId()));
        }
        Curve4PoolData v4Data = this.getVarCurve4PoolData();
        StaticArray<Uint256> amounts = (StaticArray<Uint256>) eventValues.getNonIndexedValues().get(0);
        StaticArray<Uint256> fees = (StaticArray<Uint256>) eventValues.getNonIndexedValues().get(1);
        List<Uint256> amountsNew = new ArrayList<>();
        for (int i = 0; i < N_COINS; i++) {
            BigInteger originBalance = v4Data.getBalances()[i];
            BigInteger fee = fees.getValue().get(i).getValue();
            BigInteger newBalance = originBalance.subtract(amounts.getValue().get(i).getValue());
            BigInteger newFee = fee.multiply(v4Data.getAdminFee()).divide(FEE_DENOMINATOR);
            newBalance = newBalance.subtract(newFee);
            v4Data.updateBalances(i, newBalance);
            amountsNew.add(new Uint256(newBalance));
        }
        BigInteger newTotalSupply = (BigInteger) eventValues.getNonIndexedValues().get(3).getValue();
        v4Data.setTotalSupply(newTotalSupply);
        this.isDirty = true;
        String newData = FunctionEncoder.encodeConstructor(
                Arrays.asList(
                        new StaticArray2(Uint256.class, amountsNew),
                        eventValues.getNonIndexedValues().get(1),
                        eventValues.getNonIndexedValues().get(2),
                        eventValues.getNonIndexedValues().get(3)
                )
        );
        return HandleResult.genHandleSuccessAndSend(topics, newData);
    }

    private HandleResult handleEventCommitNewAdmin(String[] topics, String data, HandleEventExtraData handleEventExtraData) {
        log.info("{}:{} handleEventCommitNewAdmin:{}", address, type, handleEventExtraData.getUniqueId());
        EventValues eventValues = getEventValue(EVENT_NAME_COMMIT_NEW_ADMIN, EVENT_NAME_COMMIT_NEW_ADMIN_BODY, topics, data,
                handleEventExtraData.getUniqueId());
        if (ObjectUtil.isNull(eventValues)) {
            return HandleResult.genHandleFailMessage(String.format("Contract%s, type:%s decode handleEventCommitNewAdmin fail!, unique id :%s",
                    address, type, handleEventExtraData.getUniqueId()));
        }
        Curve4PoolData v4Data = this.getVarCurve4PoolData();
        BigInteger deadline = (BigInteger) eventValues.getIndexedValues().get(0).getValue();
        String admin = WalletUtil.hexStringToTron((String) eventValues.getIndexedValues().get(1).getValue());
        v4Data.setOwner(admin);
        v4Data.setTransferOwnershipDeadline(deadline);
        isDirty = true;
        return HandleResult.genHandleSuccess();
    }

    private HandleResult handleEventNewAdmin(String[] topics, String data, HandleEventExtraData handleEventExtraData) {
        log.info("{}:{} handleEventNewAdmin:{}", address, type, handleEventExtraData.getUniqueId());
        EventValues eventValues = getEventValue(EVENT_NAME_NEW_ADMIN, EVENT_NAME_NEW_ADMIN_BODY, topics, data,
                handleEventExtraData.getUniqueId());
        if (ObjectUtil.isNull(eventValues)) {
            return HandleResult.genHandleFailMessage(String.format("Contract%s, type:%s decode handleEventNewAdmin fail!, unique id :%s",
                    address, type, handleEventExtraData.getUniqueId()));
        }
        String admin = WalletUtil.hexStringToTron((String) eventValues.getIndexedValues().get(0).getValue());
        Curve4PoolData v4Data = this.getVarCurve4PoolData();
        v4Data.setOwner(admin);
        v4Data.setTransferOwnershipDeadline(BigInteger.ZERO);
        isDirty = true;
        return HandleResult.genHandleSuccess();
    }

    private HandleResult handleEventNewFeeConverter(String[] topics, String data, HandleEventExtraData handleEventExtraData) {
        log.info("handleEventNewFeeConverter not implements!");
        return HandleResult.genHandleFailMessage("handleEventNewFeeConverter not implements!");
    }

    private HandleResult handleEventCommitNewFee(String[] topics, String data, HandleEventExtraData handleEventExtraData) {
        log.info("{}:{} handleEventCommitNewFee:{}", address, type, handleEventExtraData.getUniqueId());
        EventValues eventValues = getEventValue(Curve2PoolEvent.EVENT_NAME_COMMIT_NEW_FEE, EVENT_NAME_COMMIT_NEW_FEE_BODY, topics, data,
                handleEventExtraData.getUniqueId());
        if (ObjectUtil.isNull(eventValues)) {
            return HandleResult.genHandleFailMessage(String.format("Contract%s, type:%s decode handleEventCommitNewFee fail!, unique id :%s",
                    address, type, handleEventExtraData.getUniqueId()));
        }
        Curve4PoolData v4Data = this.getVarCurve4PoolData();
        BigInteger deadLine = (BigInteger) eventValues.getIndexedValues().get(0).getValue();
        BigInteger fee = (BigInteger) eventValues.getNonIndexedValues().get(0).getValue();
        BigInteger adminFee = (BigInteger) eventValues.getNonIndexedValues().get(1).getValue();
        v4Data.setFee(fee);
        v4Data.setAdminFee(adminFee);
        v4Data.setAdminActionsDeadline(deadLine);
        isDirty = true;
        return HandleResult.genHandleSuccess();
    }

    private HandleResult handleEventNewFee(String[] topics, String data, HandleEventExtraData handleEventExtraData) {
        log.info("{}:{} handleEventNewFee:{}", address, type, handleEventExtraData.getUniqueId());
        EventValues eventValues = getEventValue(EVENT_NAME_NEW_FEE, EVENT_NAME_NEW_FEE_BODY, topics, data,
                handleEventExtraData.getUniqueId());
        if (ObjectUtil.isNull(eventValues)) {
            return HandleResult.genHandleFailMessage(String.format("Contract%s, type:%s decode handleEventNewFee fail!, unique id :%s",
                    address, type, handleEventExtraData.getUniqueId()));
        }
        Curve4PoolData v4Data = this.getVarCurve4PoolData();
        BigInteger fee = (BigInteger) eventValues.getNonIndexedValues().get(0).getValue();
        BigInteger adminFee = (BigInteger) eventValues.getNonIndexedValues().get(1).getValue();
        v4Data.setFee(fee);
        v4Data.setAdminFee(adminFee);
        v4Data.setAdminActionsDeadline(BigInteger.ZERO);
        isDirty = true;
        return HandleResult.genHandleSuccess();
    }

    private HandleResult handleEventRampA(String[] topics, String data, HandleEventExtraData handleEventExtraData) {
        log.info("{}:{} handleEventRampA:{}", address, type, handleEventExtraData.getUniqueId());
        EventValues eventValues = getEventValue(EVENT_NAME_RAMP_A, EVENT_NAME_RAMP_A_BODY, topics, data,
                handleEventExtraData.getUniqueId());
        if (ObjectUtil.isNull(eventValues)) {
            return HandleResult.genHandleFailMessage(String.format("Contract%s, type:%s decode handleEventRampA fail!, unique id :%s",
                    address, type, handleEventExtraData.getUniqueId()));
        }
        Curve4PoolData v4Data = this.getVarCurve4PoolData();
        BigInteger a = (BigInteger) eventValues.getNonIndexedValues().get(0).getValue();
        BigInteger af = (BigInteger) eventValues.getNonIndexedValues().get(1).getValue();
        BigInteger aT = (BigInteger) eventValues.getNonIndexedValues().get(2).getValue();
        BigInteger afT = (BigInteger) eventValues.getNonIndexedValues().get(3).getValue();
        v4Data.setInitialA(a);
        v4Data.setInitialATime(aT);
        v4Data.setFutureATime(afT);
        v4Data.setFutureA(af);
        isDirty = true;
        return HandleResult.genHandleSuccess();
    }

    private HandleResult handleEventStopRampA(String[] topics, String data, HandleEventExtraData handleEventExtraData) {
        log.info("{}:{} handleEventStopRampA:{}", address, type, handleEventExtraData.getUniqueId());
        EventValues eventValues = getEventValue(EVENT_NAME_STOP_RAMP_A, EVENT_NAME_STOP_RAMP_A_BODY, topics, data,
                handleEventExtraData.getUniqueId());
        if (ObjectUtil.isNull(eventValues)) {
            return HandleResult.genHandleFailMessage(String.format("Contract%s, type:%s decode handleEventStopRampA fail!, unique id :%s",
                    address, type, handleEventExtraData.getUniqueId()));
        }
        Curve4PoolData v4Data = this.getVarCurve4PoolData();
        BigInteger a = (BigInteger) eventValues.getNonIndexedValues().get(0).getValue();
        BigInteger aTime = (BigInteger) eventValues.getNonIndexedValues().get(1).getValue();
        v4Data.setFutureA(a);
        v4Data.setInitialA(BigInteger.valueOf(a.longValue()));
        v4Data.setInitialATime(aTime);
        v4Data.setInitialATime(BigInteger.valueOf(aTime.longValue()));
        isDirty = true;
        return HandleResult.genHandleSuccess();
    }
}
