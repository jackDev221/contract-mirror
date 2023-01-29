package org.tron.sunio.contract_mirror.mirror.contracts.impl;

import cn.hutool.core.util.ObjectUtil;
import lombok.extern.slf4j.Slf4j;
import org.tron.sunio.contract_mirror.mirror.chainHelper.IChainHelper;
import org.tron.sunio.contract_mirror.mirror.chainHelper.TriggerContractInfo;
import org.tron.sunio.contract_mirror.mirror.consts.ContractMirrorConst;
import org.tron.sunio.contract_mirror.mirror.contracts.BaseContract;
import org.tron.sunio.contract_mirror.mirror.dao.Curve3PoolData;
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

import static org.tron.sunio.contract_mirror.event_decode.events.Curve3PoolEvent.EVENT_NAME_ADD_LIQUIDITY_BODY;
import static org.tron.sunio.contract_mirror.event_decode.events.Curve3PoolEvent.EVENT_NAME_COMMIT_NEW_ADMIN_BODY;
import static org.tron.sunio.contract_mirror.event_decode.events.Curve3PoolEvent.EVENT_NAME_COMMIT_NEW_FEE_BODY;
import static org.tron.sunio.contract_mirror.event_decode.events.Curve3PoolEvent.EVENT_NAME_NEW_ADMIN_BODY;
import static org.tron.sunio.contract_mirror.event_decode.events.Curve3PoolEvent.EVENT_NAME_NEW_FEE_BODY;
import static org.tron.sunio.contract_mirror.event_decode.events.Curve3PoolEvent.EVENT_NAME_NEW_FEE_CONVERTER_BODY;
import static org.tron.sunio.contract_mirror.event_decode.events.Curve3PoolEvent.EVENT_NAME_RAMP_A_BODY;
import static org.tron.sunio.contract_mirror.event_decode.events.Curve3PoolEvent.EVENT_NAME_REMOVE_LIQUIDITY_BODY;
import static org.tron.sunio.contract_mirror.event_decode.events.Curve3PoolEvent.EVENT_NAME_REMOVE_LIQUIDITY_IM_BALANCE_BODY;
import static org.tron.sunio.contract_mirror.event_decode.events.Curve3PoolEvent.EVENT_NAME_STOP_RAMP_A_BODY;
import static org.tron.sunio.contract_mirror.event_decode.events.Curve3PoolEvent.EVENT_NAME_TOKEN_EXCHANGE_BODY;
import static org.tron.sunio.contract_mirror.event_decode.events.Curve3PoolEvent.EVENT_NAME_ADD_LIQUIDITY;
import static org.tron.sunio.contract_mirror.event_decode.events.Curve3PoolEvent.EVENT_NAME_COMMIT_NEW_ADMIN;
import static org.tron.sunio.contract_mirror.event_decode.events.Curve3PoolEvent.EVENT_NAME_COMMIT_NEW_FEE;
import static org.tron.sunio.contract_mirror.event_decode.events.Curve3PoolEvent.EVENT_NAME_NEW_ADMIN;
import static org.tron.sunio.contract_mirror.event_decode.events.Curve3PoolEvent.EVENT_NAME_NEW_FEE;
import static org.tron.sunio.contract_mirror.event_decode.events.Curve3PoolEvent.EVENT_NAME_NEW_FEE_CONVERTER;
import static org.tron.sunio.contract_mirror.event_decode.events.Curve3PoolEvent.EVENT_NAME_RAMP_A;
import static org.tron.sunio.contract_mirror.event_decode.events.Curve3PoolEvent.EVENT_NAME_REMOVE_LIQUIDITY;
import static org.tron.sunio.contract_mirror.event_decode.events.Curve3PoolEvent.EVENT_NAME_REMOVE_LIQUIDITY_IM_BALANCE;
import static org.tron.sunio.contract_mirror.event_decode.events.Curve3PoolEvent.EVENT_NAME_REMOVE_LIQUIDITY_ONE;
import static org.tron.sunio.contract_mirror.event_decode.events.Curve3PoolEvent.EVENT_NAME_STOP_RAMP_A;
import static org.tron.sunio.contract_mirror.event_decode.events.Curve3PoolEvent.EVENT_NAME_TOKEN_EXCHANGE;

@Slf4j
public class Curve3Pool extends BaseContract {
    private static final int N_COINS = 3;
    private static final BigInteger FEE_DENOMINATOR = BigInteger.TEN.pow(10);
    private Curve3PoolData curve3PoolData;

    public Curve3Pool(String address, IChainHelper iChainHelper,  Map<String, String> sigMap) {
        super(address, ContractType.CONTRACT_CURVE_3POOL, iChainHelper, sigMap);
    }

    private Curve3PoolData getVarCurve3PoolData() {
        if (ObjectUtil.isNull(curve3PoolData)) {
            curve3PoolData = new Curve3PoolData();
            curve3PoolData.setAddress(address);
            curve3PoolData.setType(type);
            curve3PoolData.setUsing(true);
            curve3PoolData.setReady(false);
            curve3PoolData.setAddExchangeContracts(false);
        }
        return curve3PoolData;
    }

    private void updateCoinsAndBalance(Curve3PoolData curve3PoolData) {
        for (int i = 0; i < N_COINS; i++) {
            // update coins string
            TriggerContractInfo triggerContractInfo = new TriggerContractInfo(ContractMirrorConst.EMPTY_ADDRESS, address,
                    "coins", List.of(new Uint256(i)), List.of(new TypeReference<Address>() {
            })
            );
            List<Type> results = this.iChainHelper.triggerConstantContract(triggerContractInfo);
            if (results.size() == 0) {
                log.error("Get contract:{} type:{} , function:{} result len is zero", this.address, this.type, "coins");
            } else {
                curve3PoolData.updateCoins(i, WalletUtil.hexStringToTron((String) results.get(0).getValue()));
            }
            // update coins balance
            triggerContractInfo = new TriggerContractInfo(ContractMirrorConst.EMPTY_ADDRESS, address, "balances",
                    List.of(new Uint256(i)), List.of(new TypeReference<Uint256>() {
            }));
            results = this.iChainHelper.triggerConstantContract(triggerContractInfo);
            if (results.size() == 0) {
                log.error("Get contract:{} type:{} , function:{} result len is zero", this.address, this.type, "balances");
            } else {
                curve3PoolData.updateBalances(i, (BigInteger) results.get(0).getValue());
            }
        }
    }

    private void updateSupply(Curve3PoolData curve3PoolData, String tokenAddress) {
        TriggerContractInfo triggerContractInfo = new TriggerContractInfo(ContractMirrorConst.EMPTY_ADDRESS, this.getAddress(),
                tokenAddress, Collections.emptyList(), List.of(new TypeReference<Uint256>() {
        }));
        List<Type> results = this.iChainHelper.triggerConstantContract(triggerContractInfo);
        if (results.size() == 0) {
            log.error("Get contract:{} type:{} , token: {} function: totalSupply() result len is zero", this.address, this.type, tokenAddress);
        } else {
            curve3PoolData.setTotalSupply((BigInteger) results.get(0).getValue());
        }
    }

    @Override
    public boolean initDataFromChain1() {
        Curve3PoolData curve3PoolData = this.getVarCurve3PoolData();
        updateCoinsAndBalance(curve3PoolData);
        String token = WalletUtil.ethAddressToTron(callContractAddress(ContractMirrorConst.EMPTY_ADDRESS, "token").toString());
        curve3PoolData.setToken(token);
        updateSupply(curve3PoolData, token);
        BigInteger fee = callContractU256(ContractMirrorConst.EMPTY_ADDRESS, "fee");
        curve3PoolData.setFee(fee);
        BigInteger futureFee = callContractU256(ContractMirrorConst.EMPTY_ADDRESS, "future_fee");
        curve3PoolData.setFutureFee(futureFee);
        BigInteger adminFee = callContractU256(ContractMirrorConst.EMPTY_ADDRESS, "admin_fee");
        curve3PoolData.setAdminFee(adminFee);
        BigInteger futureAdminFee = callContractU256(ContractMirrorConst.EMPTY_ADDRESS, "future_admin_fee");
        curve3PoolData.setFutureAdminFee(futureAdminFee);
        BigInteger adminActionsDeadline = callContractU256(ContractMirrorConst.EMPTY_ADDRESS, "admin_actions_deadline");
        curve3PoolData.setAdminActionsDeadline(adminActionsDeadline);
        String feeConverter = WalletUtil.ethAddressToTron(callContractAddress(ContractMirrorConst.EMPTY_ADDRESS, "fee_converter").toString());
        curve3PoolData.setFeeConverter(feeConverter);
        BigInteger initialA = callContractU256(ContractMirrorConst.EMPTY_ADDRESS, "initial_A");
        curve3PoolData.setInitialA(initialA);
        BigInteger initialATime = callContractU256(ContractMirrorConst.EMPTY_ADDRESS, "initial_A_time");
        curve3PoolData.setInitialATime(initialATime);
        BigInteger futureA = callContractU256(ContractMirrorConst.EMPTY_ADDRESS, "future_A");
        curve3PoolData.setFutureA(futureA);
        BigInteger futureATime = callContractU256(ContractMirrorConst.EMPTY_ADDRESS, "future_A_time");
        curve3PoolData.setFutureATime(futureATime);
        String owner = WalletUtil.ethAddressToTron(callContractAddress(ContractMirrorConst.EMPTY_ADDRESS, "owner").toString());
        curve3PoolData.setOwner(owner);
        String futureOwner = WalletUtil.ethAddressToTron(callContractAddress(ContractMirrorConst.EMPTY_ADDRESS, "future_owner").toString());
        curve3PoolData.setFutureOwner(futureOwner);
        BigInteger transferOwnershipDeadline = callContractU256(ContractMirrorConst.EMPTY_ADDRESS, "transfer_ownership_deadline");
        curve3PoolData.setTransferOwnershipDeadline(transferOwnershipDeadline);
        isDirty = true;
        return true;
    }

    @Override
    public void updateBaseInfo(boolean isUsing, boolean isReady, boolean isAddExchangeContracts) {
        Curve3PoolData curve3PoolData = this.getVarCurve3PoolData();
        curve3PoolData.setUsing(isUsing);
        curve3PoolData.setReady(isReady);
        curve3PoolData.setAddExchangeContracts(isAddExchangeContracts);
        isDirty = true;
    }

    @Override
    protected void saveUpdateToCache() {
    }

    @Override
    protected HandleResult handleEvent1(String eventName, String[] topics, String data, HandleEventExtraData handleEventExtraData) {
        HandleResult result;
        switch (eventName) {
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
        return (T) getVarCurve3PoolData();
    }

    @Override
    public <T> T handleSpecialRequest(String method) {
        return null;
    }

    private HandleResult handleEventTokenExchange(String[] topics, String data, HandleEventExtraData handleEventExtraData) {
        Curve3PoolData curve3PoolData = this.getVarCurve3PoolData();
        if (curve3PoolData.getAdminFee().compareTo(BigInteger.ZERO) == 0) {
            EventValues eventValues = getEventValue(EVENT_NAME_TOKEN_EXCHANGE, EVENT_NAME_TOKEN_EXCHANGE_BODY,
                    topics, data, handleEventExtraData.getUniqueId());
            if (ObjectUtil.isNull(eventValues)) {
                return HandleResult.genHandleFailMessage(String.format("Contract%s, type:%s decode handleEventTokenExchange fail!, unique id :%s",
                        address, type, handleEventExtraData.getUniqueId()));
            }
            int i = ((BigInteger) eventValues.getNonIndexedValues().get(0).getValue()).intValue();
            BigInteger dx = (BigInteger) eventValues.getNonIndexedValues().get(1).getValue();
            int j = ((BigInteger) eventValues.getNonIndexedValues().get(0).getValue()).intValue();
            BigInteger dy = (BigInteger) eventValues.getNonIndexedValues().get(1).getValue();

            BigInteger newIBalance = curve3PoolData.getBalance()[i].add(dx);
            BigInteger newJBalance = curve3PoolData.getBalance()[j].subtract(dy);
            curve3PoolData.updateBalances(i, newIBalance);
            curve3PoolData.updateBalances(j, newJBalance);
        } else {
            // balances 变化无法推断，重新更新数据。TODO add dy ===>dy_admin_fee
            updateBaseInfo(isUsing, false, isAddExchangeContracts);
            this.isReady = false;
        }
        this.isDirty = true;
        return HandleResult.genHandleSuccess();
    }

    private HandleResult handleEventAddLiquidity(String[] topics, String data, HandleEventExtraData handleEventExtraData) {
        // TODO 暂时 默认没有特殊的收费ERC20 特殊收费ERC20处理
        EventValues eventValues = getEventValue(EVENT_NAME_ADD_LIQUIDITY, EVENT_NAME_ADD_LIQUIDITY_BODY,
                topics, data, handleEventExtraData.getUniqueId());
        if (ObjectUtil.isNull(eventValues)) {
            return HandleResult.genHandleFailMessage(String.format("Contract%s, type:%s decode handleEventAddLiquidity fail!, unique id :%s",
                    address, type, handleEventExtraData.getUniqueId()));
        }
        Curve3PoolData curve3PoolData = this.getVarCurve3PoolData();
        StaticArray<Uint256> amounts = (StaticArray<Uint256>) eventValues.getNonIndexedValues().get(0);
        StaticArray<Uint256> fees = (StaticArray<Uint256>) eventValues.getNonIndexedValues().get(1);
        List<Uint256> amountsNew = new ArrayList<>();
        for (int i = 0; i < N_COINS; i++) {
            BigInteger originBalance = curve3PoolData.getBalance()[i];
            BigInteger fee = fees.getValue().get(i).getValue();
            BigInteger newBalance = originBalance.add(amounts.getValue().get(i).getValue());
            BigInteger newFee = fee.multiply(curve3PoolData.getAdminFee()).divide(FEE_DENOMINATOR);
            newBalance = newBalance.subtract(newFee);
            curve3PoolData.updateBalances(i, newBalance);
            amountsNew.add(new Uint256(newBalance));
        }
        BigInteger newTotalSupply = (BigInteger) eventValues.getNonIndexedValues().get(3).getValue();
        curve3PoolData.setTotalSupply(newTotalSupply);
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
        EventValues eventValues = getEventValue(EVENT_NAME_REMOVE_LIQUIDITY, EVENT_NAME_REMOVE_LIQUIDITY_BODY,
                topics, data, handleEventExtraData.getUniqueId());
        if (ObjectUtil.isNull(eventValues)) {
            return HandleResult.genHandleFailMessage(String.format("Contract%s, type:%s decode handleEventRemoveLiquidity fail!, unique id :%s",
                    address, type, handleEventExtraData.getUniqueId()));
        }
        Curve3PoolData curve3PoolData = this.getVarCurve3PoolData();
        StaticArray<Uint256> amounts = (StaticArray<Uint256>) eventValues.getNonIndexedValues().get(0);
        List<Uint256> amountsNew = new ArrayList<>();
        for (int i = 0; i < N_COINS; i++) {
            BigInteger origin = curve3PoolData.getBalance()[i];
            BigInteger newBalance = origin.subtract((BigInteger) amounts.getValue().get(0).getValue());
            curve3PoolData.updateBalances(i, newBalance);
            amountsNew.add(new Uint256(newBalance));
        }
        BigInteger newTotalSupply = (BigInteger) eventValues.getNonIndexedValues().get(2).getValue();
        curve3PoolData.setTotalSupply(newTotalSupply);
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
        updateBaseInfo(isUsing, false, isAddExchangeContracts);
        this.isReady = false;
        this.isDirty = true;
        return HandleResult.genHandleSuccess();
    }

    private HandleResult handleEventRemoveLiquidityImbalance(String[] topics, String data, HandleEventExtraData handleEventExtraData) {
        EventValues eventValues = getEventValue(EVENT_NAME_REMOVE_LIQUIDITY_IM_BALANCE, EVENT_NAME_REMOVE_LIQUIDITY_IM_BALANCE_BODY,
                topics, data, handleEventExtraData.getUniqueId());
        if (ObjectUtil.isNull(eventValues)) {
            return HandleResult.genHandleFailMessage(String.format("Contract%s, type:%s decode handleEventRemoveLiquidityImbalance fail!, unique id :%s",
                    address, type, handleEventExtraData.getUniqueId()));
        }
        Curve3PoolData curve3PoolData = this.getVarCurve3PoolData();
        StaticArray<Uint256> amounts = (StaticArray<Uint256>) eventValues.getNonIndexedValues().get(0);
        StaticArray<Uint256> fees = (StaticArray<Uint256>) eventValues.getNonIndexedValues().get(1);
        List<Uint256> amountsNew = new ArrayList<>();
        for (int i = 0; i < N_COINS; i++) {
            BigInteger originBalance = curve3PoolData.getBalance()[i];
            BigInteger fee = fees.getValue().get(i).getValue();
            BigInteger newBalance = originBalance.subtract(amounts.getValue().get(i).getValue());
            BigInteger newFee = fee.multiply(curve3PoolData.getAdminFee()).divide(FEE_DENOMINATOR);
            newBalance = newBalance.subtract(newFee);
            curve3PoolData.updateBalances(i, newBalance);
            amountsNew.add(new Uint256(newBalance));
        }
        BigInteger newTotalSupply = (BigInteger) eventValues.getNonIndexedValues().get(3).getValue();
        curve3PoolData.setTotalSupply(newTotalSupply);
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
        EventValues eventValues = getEventValue(EVENT_NAME_COMMIT_NEW_ADMIN, EVENT_NAME_COMMIT_NEW_ADMIN_BODY,
                topics, data, handleEventExtraData.getUniqueId());
        if (ObjectUtil.isNull(eventValues)) {
            return HandleResult.genHandleFailMessage(String.format("Contract%s, type:%s decode handleEventCommitNewAdmin fail!, unique id :%s",
                    address, type, handleEventExtraData.getUniqueId()));
        }
        Curve3PoolData curve3PoolData = this.getVarCurve3PoolData();
        BigInteger deadline = (BigInteger) eventValues.getIndexedValues().get(0).getValue();
        String admin = WalletUtil.hexStringToTron((String) eventValues.getIndexedValues().get(1).getValue());
        curve3PoolData.setOwner(admin);
        curve3PoolData.setTransferOwnershipDeadline(deadline);
        isDirty = true;
        return HandleResult.genHandleSuccess();
    }

    private HandleResult handleEventNewAdmin(String[] topics, String data, HandleEventExtraData handleEventExtraData) {
        EventValues eventValues = getEventValue(EVENT_NAME_NEW_ADMIN, EVENT_NAME_NEW_ADMIN_BODY,
                topics, data, handleEventExtraData.getUniqueId());
        if (ObjectUtil.isNull(eventValues)) {
            return HandleResult.genHandleFailMessage(String.format("Contract%s, type:%s decode handleEventNewAdmin fail!, unique id :%s",
                    address, type, handleEventExtraData.getUniqueId()));
        }
        String admin = WalletUtil.hexStringToTron((String) eventValues.getIndexedValues().get(0).getValue());
        Curve3PoolData curve3PoolData = this.getVarCurve3PoolData();
        curve3PoolData.setOwner(admin);
        curve3PoolData.setTransferOwnershipDeadline(BigInteger.ZERO);
        isDirty = true;
        return HandleResult.genHandleSuccess();
    }

    private HandleResult handleEventNewFeeConverter(String[] topics, String data, HandleEventExtraData handleEventExtraData) {
        EventValues eventValues = getEventValue(EVENT_NAME_NEW_FEE_CONVERTER, EVENT_NAME_NEW_FEE_CONVERTER_BODY,
                topics, data, handleEventExtraData.getUniqueId());
        if (ObjectUtil.isNull(eventValues)) {
            return HandleResult.genHandleFailMessage(String.format("Contract%s, type:%s decode handleEventNewFeeConverter fail!, unique id :%s",
                    address, type, handleEventExtraData.getUniqueId()));
        }
        String feeConverter = WalletUtil.hexStringToTron((String) eventValues.getIndexedValues().get(0).getValue());
        Curve3PoolData curve3PoolData = this.getVarCurve3PoolData();
        curve3PoolData.setFeeConverter(feeConverter);
        isDirty = true;
        return HandleResult.genHandleSuccess();
    }

    private HandleResult handleEventCommitNewFee(String[] topics, String data, HandleEventExtraData handleEventExtraData) {
        EventValues eventValues = getEventValue(EVENT_NAME_COMMIT_NEW_FEE, EVENT_NAME_COMMIT_NEW_FEE_BODY,
                topics, data, handleEventExtraData.getUniqueId());
        if (ObjectUtil.isNull(eventValues)) {
            return HandleResult.genHandleFailMessage(String.format("Contract%s, type:%s decode handleEventCommitNewFee fail!, unique id :%s",
                    address, type, handleEventExtraData.getUniqueId()));
        }
        Curve3PoolData curve3PoolData = this.getVarCurve3PoolData();
        BigInteger deadLine = (BigInteger) eventValues.getIndexedValues().get(0).getValue();
        BigInteger fee = (BigInteger) eventValues.getNonIndexedValues().get(0).getValue();
        BigInteger adminFee = (BigInteger) eventValues.getNonIndexedValues().get(1).getValue();
        curve3PoolData.setFee(fee);
        curve3PoolData.setAdminFee(adminFee);
        curve3PoolData.setAdminActionsDeadline(deadLine);
        isDirty = true;
        return HandleResult.genHandleSuccess();
    }

    private HandleResult handleEventNewFee(String[] topics, String data, HandleEventExtraData handleEventExtraData) {
        EventValues eventValues = getEventValue(EVENT_NAME_NEW_FEE, EVENT_NAME_NEW_FEE_BODY,
                topics, data, handleEventExtraData.getUniqueId());
        if (ObjectUtil.isNull(eventValues)) {
            return HandleResult.genHandleFailMessage(String.format("Contract%s, type:%s decode handleEventNewFee fail!, unique id :%s",
                    address, type, handleEventExtraData.getUniqueId()));
        }
        Curve3PoolData curve3PoolData = this.getVarCurve3PoolData();
        BigInteger fee = (BigInteger) eventValues.getNonIndexedValues().get(0).getValue();
        BigInteger adminFee = (BigInteger) eventValues.getNonIndexedValues().get(1).getValue();
        curve3PoolData.setFee(fee);
        curve3PoolData.setAdminFee(adminFee);
        curve3PoolData.setAdminActionsDeadline(BigInteger.ZERO);
        isDirty = true;
        return HandleResult.genHandleSuccess();
    }

    private HandleResult handleEventRampA(String[] topics, String data, HandleEventExtraData handleEventExtraData) {
        EventValues eventValues = getEventValue(EVENT_NAME_RAMP_A, EVENT_NAME_RAMP_A_BODY,
                topics, data, handleEventExtraData.getUniqueId());
        if (ObjectUtil.isNull(eventValues)) {
            return HandleResult.genHandleFailMessage(String.format("Contract%s, type:%s decode handleEventRampA fail!, unique id :%s",
                    address, type, handleEventExtraData.getUniqueId()));
        }
        Curve3PoolData curve3PoolData = this.getVarCurve3PoolData();
        BigInteger a = (BigInteger) eventValues.getNonIndexedValues().get(0).getValue();
        BigInteger af = (BigInteger) eventValues.getNonIndexedValues().get(1).getValue();
        BigInteger aT = (BigInteger) eventValues.getNonIndexedValues().get(2).getValue();
        BigInteger afT = (BigInteger) eventValues.getNonIndexedValues().get(3).getValue();
        curve3PoolData.setInitialA(a);
        curve3PoolData.setInitialATime(aT);
        curve3PoolData.setFutureATime(afT);
        curve3PoolData.setFutureA(af);
        isDirty = true;
        return HandleResult.genHandleSuccess();
    }

    private HandleResult handleEventStopRampA(String[] topics, String data, HandleEventExtraData handleEventExtraData) {
        EventValues eventValues = getEventValue(EVENT_NAME_STOP_RAMP_A, EVENT_NAME_STOP_RAMP_A_BODY,
                topics, data, handleEventExtraData.getUniqueId());
        if (ObjectUtil.isNull(eventValues)) {
            return HandleResult.genHandleFailMessage(String.format("Contract%s, type:%s decode handleEventStopRampA fail!, unique id :%s",
                    address, type, handleEventExtraData.getUniqueId()));
        }
        Curve3PoolData curve3PoolData = this.getVarCurve3PoolData();
        BigInteger a = (BigInteger) eventValues.getNonIndexedValues().get(0).getValue();
        BigInteger aTime = (BigInteger) eventValues.getNonIndexedValues().get(1).getValue();
        curve3PoolData.setFutureA(a);
        curve3PoolData.setInitialA(BigInteger.valueOf(a.longValue()));
        curve3PoolData.setInitialATime(aTime);
        curve3PoolData.setInitialATime(BigInteger.valueOf(aTime.longValue()));
        isDirty = true;
        return HandleResult.genHandleSuccess();
    }
}
