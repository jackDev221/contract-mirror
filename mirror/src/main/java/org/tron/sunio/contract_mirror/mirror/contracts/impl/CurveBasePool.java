package org.tron.sunio.contract_mirror.mirror.contracts.impl;

import cn.hutool.core.util.ObjectUtil;
import lombok.extern.slf4j.Slf4j;
import org.tron.sunio.contract_mirror.mirror.chainHelper.IChainHelper;
import org.tron.sunio.contract_mirror.mirror.chainHelper.TriggerContractInfo;
import org.tron.sunio.contract_mirror.mirror.consts.ContractMirrorConst;
import org.tron.sunio.contract_mirror.mirror.contracts.BaseContract;
import org.tron.sunio.contract_mirror.mirror.dao.CurveBasePoolData;
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

import static org.tron.sunio.contract_mirror.event_decode.events.Curve2PoolEvent.EVENT_NAME_ADD_LIQUIDITY;
import static org.tron.sunio.contract_mirror.event_decode.events.Curve2PoolEvent.EVENT_NAME_ADD_LIQUIDITY_BODY;
import static org.tron.sunio.contract_mirror.event_decode.events.Curve2PoolEvent.EVENT_NAME_COMMIT_NEW_ADMIN;
import static org.tron.sunio.contract_mirror.event_decode.events.Curve2PoolEvent.EVENT_NAME_COMMIT_NEW_ADMIN_BODY;
import static org.tron.sunio.contract_mirror.event_decode.events.Curve2PoolEvent.EVENT_NAME_COMMIT_NEW_FEE;
import static org.tron.sunio.contract_mirror.event_decode.events.Curve2PoolEvent.EVENT_NAME_COMMIT_NEW_FEE_BODY;
import static org.tron.sunio.contract_mirror.event_decode.events.Curve2PoolEvent.EVENT_NAME_NEW_ADMIN;
import static org.tron.sunio.contract_mirror.event_decode.events.Curve2PoolEvent.EVENT_NAME_NEW_ADMIN_BODY;
import static org.tron.sunio.contract_mirror.event_decode.events.Curve2PoolEvent.EVENT_NAME_NEW_FEE;
import static org.tron.sunio.contract_mirror.event_decode.events.Curve2PoolEvent.EVENT_NAME_NEW_FEE_BODY;
import static org.tron.sunio.contract_mirror.event_decode.events.Curve2PoolEvent.EVENT_NAME_NEW_FEE_CONVERTER;
import static org.tron.sunio.contract_mirror.event_decode.events.Curve2PoolEvent.EVENT_NAME_NEW_FEE_CONVERTER_BODY;
import static org.tron.sunio.contract_mirror.event_decode.events.Curve2PoolEvent.EVENT_NAME_RAMP_A;
import static org.tron.sunio.contract_mirror.event_decode.events.Curve2PoolEvent.EVENT_NAME_RAMP_A_BODY;
import static org.tron.sunio.contract_mirror.event_decode.events.Curve2PoolEvent.EVENT_NAME_REMOVE_LIQUIDITY;
import static org.tron.sunio.contract_mirror.event_decode.events.Curve2PoolEvent.EVENT_NAME_REMOVE_LIQUIDITY_BODY;
import static org.tron.sunio.contract_mirror.event_decode.events.Curve2PoolEvent.EVENT_NAME_REMOVE_LIQUIDITY_IM_BALANCE;
import static org.tron.sunio.contract_mirror.event_decode.events.Curve2PoolEvent.EVENT_NAME_REMOVE_LIQUIDITY_IM_BALANCE_BODY;
import static org.tron.sunio.contract_mirror.event_decode.events.Curve2PoolEvent.EVENT_NAME_REMOVE_LIQUIDITY_ONE;
import static org.tron.sunio.contract_mirror.event_decode.events.Curve2PoolEvent.EVENT_NAME_STOP_RAMP_A;
import static org.tron.sunio.contract_mirror.event_decode.events.Curve2PoolEvent.EVENT_NAME_STOP_RAMP_A_BODY;
import static org.tron.sunio.contract_mirror.event_decode.events.Curve2PoolEvent.EVENT_NAME_TOKEN_EXCHANGE;
import static org.tron.sunio.contract_mirror.event_decode.events.Curve2PoolEvent.EVENT_NAME_TOKEN_EXCHANGE_BODY;
import static org.tron.sunio.contract_mirror.mirror.consts.ContractMirrorConst.METHOD_ADMIN_ACTIONS_DEADLINE;
import static org.tron.sunio.contract_mirror.mirror.consts.ContractMirrorConst.METHOD_ADMIN_FEE;
import static org.tron.sunio.contract_mirror.mirror.consts.ContractMirrorConst.METHOD_FEE;
import static org.tron.sunio.contract_mirror.mirror.consts.ContractMirrorConst.METHOD_FEE_CONVERTER;
import static org.tron.sunio.contract_mirror.mirror.consts.ContractMirrorConst.METHOD_FUTURE_A;
import static org.tron.sunio.contract_mirror.mirror.consts.ContractMirrorConst.METHOD_FUTURE_ADMIN_FEE;
import static org.tron.sunio.contract_mirror.mirror.consts.ContractMirrorConst.METHOD_FUTURE_A_TIME;
import static org.tron.sunio.contract_mirror.mirror.consts.ContractMirrorConst.METHOD_FUTURE_FEE;
import static org.tron.sunio.contract_mirror.mirror.consts.ContractMirrorConst.METHOD_FUTURE_OWNER;
import static org.tron.sunio.contract_mirror.mirror.consts.ContractMirrorConst.METHOD_INITIAL_A;
import static org.tron.sunio.contract_mirror.mirror.consts.ContractMirrorConst.METHOD_INITIAL_A_TIME;
import static org.tron.sunio.contract_mirror.mirror.consts.ContractMirrorConst.METHOD_OWNER;
import static org.tron.sunio.contract_mirror.mirror.consts.ContractMirrorConst.METHOD_TOKEN;
import static org.tron.sunio.contract_mirror.mirror.consts.ContractMirrorConst.METHOD_TRANSFER_OWNERSHIP_DEADLINE;

@Slf4j
public class CurveBasePool extends BaseContract {
    protected int coinsCount;
    private static final BigInteger FEE_DENOMINATOR = BigInteger.TEN.pow(10);
    protected CurveBasePoolData curveBasePoolData;

    public CurveBasePool(String address, ContractType type, IChainHelper iChainHelper, int coinsCount, Map<String, String> sigMap) {
        super(address, type, iChainHelper, sigMap);
        this.coinsCount = coinsCount;
    }

    protected CurveBasePoolData getVarCurveBasePoolData() {
        if (ObjectUtil.isNull(curveBasePoolData)) {
            curveBasePoolData = new CurveBasePoolData(coinsCount);
            curveBasePoolData.setAddress(address);
            curveBasePoolData.setType(type);
            curveBasePoolData.setUsing(true);
            curveBasePoolData.setReady(false);
            curveBasePoolData.setAddExchangeContracts(false);
        }
        return curveBasePoolData;
    }

    protected void updateCoinsAndBalance(CurveBasePoolData curveBasePoolData) {
        for (int i = 0; i < coinsCount; i++) {
            // update coins string
            TriggerContractInfo triggerContractInfo = new TriggerContractInfo(ContractMirrorConst.EMPTY_ADDRESS, address,
                    "coins", List.of(new Uint256(i)), List.of(new TypeReference<Address>() {
            })
            );
            List<Type> results = this.iChainHelper.triggerConstantContract(triggerContractInfo);
            if (results.size() == 0) {
                log.error("Get contract:{} type:{} , function:{} result len is zero", this.address, this.type, "coins");
            } else {
                curveBasePoolData.updateCoins(i, WalletUtil.hexStringToTron((String) results.get(0).getValue()));
            }
            // update coins balance
            triggerContractInfo = new TriggerContractInfo(ContractMirrorConst.EMPTY_ADDRESS, address, "balances",
                    List.of(new Uint256(i)), List.of(new TypeReference<Uint256>() {
            }));
            results = this.iChainHelper.triggerConstantContract(triggerContractInfo);
            if (results.size() == 0) {
                log.error("Get contract:{} type:{} , function:{} result len is zero", this.address, this.type, "balances");
            } else {
                curveBasePoolData.updateBalances(i, (BigInteger) results.get(0).getValue());
            }
        }
    }

    protected void updateSupply(CurveBasePoolData curveBasePoolData, String tokenAddress) {
        TriggerContractInfo triggerContractInfo = new TriggerContractInfo(ContractMirrorConst.EMPTY_ADDRESS, this.getAddress(),
                tokenAddress, Collections.emptyList(), List.of(new TypeReference<Uint256>() {
        }));
        List<Type> results = this.iChainHelper.triggerConstantContract(triggerContractInfo);
        if (results.size() == 0) {
            log.error("Get contract:{} type:{} , token: {} function: totalSupply() result len is zero", this.address, this.type, tokenAddress);
        } else {
            curveBasePoolData.setTotalSupply((BigInteger) results.get(0).getValue());
        }
    }

    @Override
    public boolean initDataFromChain1() {
        CurveBasePoolData curveBasePoolData = this.getVarCurveBasePoolData();
        updateCoinsAndBalance(curveBasePoolData);
        String token = WalletUtil.ethAddressToTron(callContractAddress(ContractMirrorConst.EMPTY_ADDRESS, "token").toString());
        curveBasePoolData.setToken(token);
        updateSupply(curveBasePoolData, token);
        BigInteger fee = callContractU256(ContractMirrorConst.EMPTY_ADDRESS, "fee");
        curveBasePoolData.setFee(fee);
        BigInteger futureFee = callContractU256(ContractMirrorConst.EMPTY_ADDRESS, "future_fee");
        curveBasePoolData.setFutureFee(futureFee);
        BigInteger adminFee = callContractU256(ContractMirrorConst.EMPTY_ADDRESS, "admin_fee");
        curveBasePoolData.setAdminFee(adminFee);
        BigInteger futureAdminFee = callContractU256(ContractMirrorConst.EMPTY_ADDRESS, "future_admin_fee");
        curveBasePoolData.setFutureAdminFee(futureAdminFee);
        BigInteger adminActionsDeadline = callContractU256(ContractMirrorConst.EMPTY_ADDRESS, "admin_actions_deadline");
        curveBasePoolData.setAdminActionsDeadline(adminActionsDeadline);
        String feeConverter = WalletUtil.ethAddressToTron(callContractAddress(ContractMirrorConst.EMPTY_ADDRESS, "fee_converter").toString());
        curveBasePoolData.setFeeConverter(feeConverter);
        BigInteger initialA = callContractU256(ContractMirrorConst.EMPTY_ADDRESS, "initial_A");
        curveBasePoolData.setInitialA(initialA);
        BigInteger initialATime = callContractU256(ContractMirrorConst.EMPTY_ADDRESS, "initial_A_time");
        curveBasePoolData.setInitialATime(initialATime);
        BigInteger futureA = callContractU256(ContractMirrorConst.EMPTY_ADDRESS, "future_A");
        curveBasePoolData.setFutureA(futureA);
        BigInteger futureATime = callContractU256(ContractMirrorConst.EMPTY_ADDRESS, "future_A_time");
        curveBasePoolData.setFutureATime(futureATime);
        String owner = WalletUtil.ethAddressToTron(callContractAddress(ContractMirrorConst.EMPTY_ADDRESS, "owner").toString());
        curveBasePoolData.setOwner(owner);
        String futureOwner = WalletUtil.ethAddressToTron(callContractAddress(ContractMirrorConst.EMPTY_ADDRESS, "future_owner").toString());
        curveBasePoolData.setFutureOwner(futureOwner);
        BigInteger transferOwnershipDeadline = callContractU256(ContractMirrorConst.EMPTY_ADDRESS, "transfer_ownership_deadline");
        curveBasePoolData.setTransferOwnershipDeadline(transferOwnershipDeadline);
        isDirty = true;
        return true;
    }

    @Override
    public void updateBaseInfo(boolean isUsing, boolean isReady, boolean isAddExchangeContracts) {
        CurveBasePoolData curveBasePoolData = this.getVarCurveBasePoolData();
        curveBasePoolData.setUsing(isUsing);
        curveBasePoolData.setReady(isReady);
        curveBasePoolData.setAddExchangeContracts(isAddExchangeContracts);
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
        return (T) getVarCurveBasePoolData();
    }

    @Override
    public <T> T handleSpecialRequest(String method) {
        switch (method) {
            case METHOD_TOKEN:
                return (T) this.getVarCurveBasePoolData().getToken();
            case METHOD_FEE:
                return (T) this.getVarCurveBasePoolData().getFee();
            case METHOD_FUTURE_FEE:
                return (T) this.getVarCurveBasePoolData().getFutureFee();
            case METHOD_ADMIN_FEE:
                return (T) this.getVarCurveBasePoolData().getAdminFee();
            case METHOD_FUTURE_ADMIN_FEE:
                return (T) this.getVarCurveBasePoolData().getFutureAdminFee();
            case METHOD_ADMIN_ACTIONS_DEADLINE:
                return (T) this.getVarCurveBasePoolData().getAdminActionsDeadline();
            case METHOD_FEE_CONVERTER:
                return (T) this.getVarCurveBasePoolData().getFeeConverter();
            case METHOD_INITIAL_A:
                return (T) this.getVarCurveBasePoolData().getInitialA();
            case METHOD_INITIAL_A_TIME:
                return (T) this.getVarCurveBasePoolData().getInitialATime();
            case METHOD_FUTURE_A:
                return (T) this.getVarCurveBasePoolData().getFutureA();
            case METHOD_FUTURE_A_TIME:
                return (T) this.getVarCurveBasePoolData().getFutureATime();
            case METHOD_OWNER:
                return (T) this.getVarCurveBasePoolData().getOwner();
            case METHOD_FUTURE_OWNER:
                return (T) this.getVarCurveBasePoolData().getFutureOwner();
            case METHOD_TRANSFER_OWNERSHIP_DEADLINE:
                return (T) this.getVarCurveBasePoolData().getTransferOwnershipDeadline();
        }
        return null;
    }

    private HandleResult handleEventTokenExchange(String[] topics, String data, HandleEventExtraData handleEventExtraData) {
        CurveBasePoolData curveBasePoolData = this.getVarCurveBasePoolData();
        if (curveBasePoolData.getAdminFee().compareTo(BigInteger.ZERO) == 0) {
            EventValues eventValues = getEventValue(EVENT_NAME_TOKEN_EXCHANGE, EVENT_NAME_TOKEN_EXCHANGE_BODY, topics, data,
                    handleEventExtraData.getUniqueId());
            if (ObjectUtil.isNull(eventValues)) {
                return HandleResult.genHandleFailMessage(String.format("Contract%s, type:%s decode handleEventTokenExchange fail!, unique id :%s",
                        address, type, handleEventExtraData.getUniqueId()));
            }
            int i = ((BigInteger) eventValues.getNonIndexedValues().get(0).getValue()).intValue();
            BigInteger dx = (BigInteger) eventValues.getNonIndexedValues().get(1).getValue();
            int j = ((BigInteger) eventValues.getNonIndexedValues().get(0).getValue()).intValue();
            BigInteger dy = (BigInteger) eventValues.getNonIndexedValues().get(1).getValue();

            BigInteger newIBalance = curveBasePoolData.getBalance()[i].add(dx);
            BigInteger newJBalance = curveBasePoolData.getBalance()[j].subtract(dy);
            curveBasePoolData.updateBalances(i, newIBalance);
            curveBasePoolData.updateBalances(j, newJBalance);
        } else {
            // balances 变化无法推断，重新更新数据。TODO add dy ===>dy_admin_fee
            updateBaseInfo(isUsing, false, isAddExchangeContracts);
            this.isReady = false;
        }
        this.isDirty = true;
        return HandleResult.genHandleSuccess();
    }

    protected HandleResult handleEventAddLiquidity(String[] topics, String data, HandleEventExtraData handleEventExtraData) {
        // TODO  暂时默认没有特殊的收费ERC20 特殊收费ERC20处理
        EventValues eventValues = getEventValue(EVENT_NAME_ADD_LIQUIDITY, EVENT_NAME_ADD_LIQUIDITY_BODY, topics, data,
                handleEventExtraData.getUniqueId());
        if (ObjectUtil.isNull(eventValues)) {
            return HandleResult.genHandleFailMessage(String.format("Contract%s, type:%s decode handleEventAddLiquidity fail!, unique id :%s",
                    address, type, handleEventExtraData.getUniqueId()));
        }
        CurveBasePoolData curveBasePoolData = this.getVarCurveBasePoolData();
        StaticArray<Uint256> amounts = (StaticArray<Uint256>) eventValues.getNonIndexedValues().get(0);
        StaticArray<Uint256> fees = (StaticArray<Uint256>) eventValues.getNonIndexedValues().get(1);
        List<Uint256> amountsNew = new ArrayList<>();
        for (int i = 0; i < coinsCount; i++) {
            BigInteger originBalance = curveBasePoolData.getBalance()[i];
            BigInteger fee = fees.getValue().get(i).getValue();
            BigInteger newBalance = originBalance.add(amounts.getValue().get(i).getValue());
            BigInteger newFee = fee.multiply(curveBasePoolData.getAdminFee()).divide(FEE_DENOMINATOR);
            newBalance = newBalance.subtract(newFee);
            curveBasePoolData.updateBalances(i, newBalance);
            amountsNew.add(new Uint256(newBalance));
        }
        BigInteger newTotalSupply = (BigInteger) eventValues.getNonIndexedValues().get(3).getValue();
        curveBasePoolData.setTotalSupply(newTotalSupply);
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

    protected HandleResult handleEventRemoveLiquidity(String[] topics, String data, HandleEventExtraData handleEventExtraData) {
        EventValues eventValues = getEventValue(EVENT_NAME_REMOVE_LIQUIDITY, EVENT_NAME_REMOVE_LIQUIDITY_BODY, topics, data,
                handleEventExtraData.getUniqueId());
        if (ObjectUtil.isNull(eventValues)) {
            return HandleResult.genHandleFailMessage(String.format("Contract%s, type:%s decode handleEventRemoveLiquidity fail!, unique id :%s",
                    address, type, handleEventExtraData.getUniqueId()));
        }
        CurveBasePoolData curveBasePoolData = this.getVarCurveBasePoolData();
        StaticArray<Uint256> amounts = (StaticArray<Uint256>) eventValues.getNonIndexedValues().get(0);
        List<Uint256> amountsNew = new ArrayList<>();
        for (int i = 0; i < coinsCount; i++) {
            BigInteger origin = curveBasePoolData.getBalance()[i];
            BigInteger newBalance = origin.subtract((BigInteger) amounts.getValue().get(0).getValue());
            curveBasePoolData.updateBalances(i, newBalance);
            amountsNew.add(new Uint256(newBalance));
        }
        BigInteger newTotalSupply = (BigInteger) eventValues.getNonIndexedValues().get(2).getValue();
        curveBasePoolData.setTotalSupply(newTotalSupply);
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

    protected HandleResult handleEventRemoveLiquidityOne(String[] topics, String data, HandleEventExtraData handleEventExtraData) {
        updateBaseInfo(isUsing, false, isAddExchangeContracts);
        this.isReady = false;
        this.isDirty = true;
        return HandleResult.genHandleSuccess();
    }

    protected HandleResult handleEventRemoveLiquidityImbalance(String[] topics, String data, HandleEventExtraData handleEventExtraData) {
        EventValues eventValues = getEventValue(EVENT_NAME_REMOVE_LIQUIDITY_IM_BALANCE, EVENT_NAME_REMOVE_LIQUIDITY_IM_BALANCE_BODY,
                topics, data, handleEventExtraData.getUniqueId());
        if (ObjectUtil.isNull(eventValues)) {
            return HandleResult.genHandleFailMessage(String.format("Contract%s, type:%s decode handleEventRemoveLiquidityImbalance fail!, unique id :%s",
                    address, type, handleEventExtraData.getUniqueId()));
        }
        CurveBasePoolData curveBasePoolData = this.getVarCurveBasePoolData();
        StaticArray<Uint256> amounts = (StaticArray<Uint256>) eventValues.getNonIndexedValues().get(0);
        StaticArray<Uint256> fees = (StaticArray<Uint256>) eventValues.getNonIndexedValues().get(1);
        List<Uint256> amountsNew = new ArrayList<>();
        for (int i = 0; i < coinsCount; i++) {
            BigInteger originBalance = curveBasePoolData.getBalance()[i];
            BigInteger fee = fees.getValue().get(i).getValue();
            BigInteger newBalance = originBalance.subtract(amounts.getValue().get(i).getValue());
            BigInteger newFee = fee.multiply(curveBasePoolData.getAdminFee()).divide(FEE_DENOMINATOR);
            newBalance = newBalance.subtract(newFee);
            curveBasePoolData.updateBalances(i, newBalance);
            amountsNew.add(new Uint256(newBalance));
        }
        BigInteger newTotalSupply = (BigInteger) eventValues.getNonIndexedValues().get(3).getValue();
        curveBasePoolData.setTotalSupply(newTotalSupply);
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

    protected HandleResult handleEventCommitNewAdmin(String[] topics, String data, HandleEventExtraData handleEventExtraData) {
        EventValues eventValues = getEventValue(EVENT_NAME_COMMIT_NEW_ADMIN, EVENT_NAME_COMMIT_NEW_ADMIN_BODY, topics, data,
                handleEventExtraData.getUniqueId());
        if (ObjectUtil.isNull(eventValues)) {
            return HandleResult.genHandleFailMessage(String.format("Contract%s, type:%s decode handleEventCommitNewAdmin fail!, unique id :%s",
                    address, type, handleEventExtraData.getUniqueId()));
        }
        CurveBasePoolData curveBasePoolData = this.getVarCurveBasePoolData();
        BigInteger deadline = (BigInteger) eventValues.getIndexedValues().get(0).getValue();
        String admin = WalletUtil.hexStringToTron((String) eventValues.getIndexedValues().get(1).getValue());
        curveBasePoolData.setOwner(admin);
        curveBasePoolData.setTransferOwnershipDeadline(deadline);
        isDirty = true;
        return HandleResult.genHandleSuccess();
    }

    protected HandleResult handleEventNewAdmin(String[] topics, String data, HandleEventExtraData handleEventExtraData) {
        EventValues eventValues = getEventValue(EVENT_NAME_NEW_ADMIN, EVENT_NAME_NEW_ADMIN_BODY, topics, data,
                handleEventExtraData.getUniqueId());
        if (ObjectUtil.isNull(eventValues)) {
            return HandleResult.genHandleFailMessage(String.format("Contract%s, type:%s decode handleEventNewAdmin fail!, unique id :%s",
                    address, type, handleEventExtraData.getUniqueId()));
        }
        String admin = WalletUtil.hexStringToTron((String) eventValues.getIndexedValues().get(0).getValue());
        CurveBasePoolData curveBasePoolData = this.getVarCurveBasePoolData();
        curveBasePoolData.setOwner(admin);
        curveBasePoolData.setTransferOwnershipDeadline(BigInteger.ZERO);
        isDirty = true;
        return HandleResult.genHandleSuccess();
    }

    protected HandleResult handleEventNewFeeConverter(String[] topics, String data, HandleEventExtraData handleEventExtraData) {
        EventValues eventValues = getEventValue(EVENT_NAME_NEW_FEE_CONVERTER, EVENT_NAME_NEW_FEE_CONVERTER_BODY, topics, data,
                handleEventExtraData.getUniqueId());
        if (ObjectUtil.isNull(eventValues)) {
            return HandleResult.genHandleFailMessage(String.format("Contract%s, type:%s decode handleEventNewFeeConverter fail!, unique id :%s",
                    address, type, handleEventExtraData.getUniqueId()));
        }
        String feeConverter = WalletUtil.hexStringToTron((String) eventValues.getIndexedValues().get(0).getValue());
        CurveBasePoolData curveBasePoolData = this.getVarCurveBasePoolData();
        curveBasePoolData.setFeeConverter(feeConverter);
        isDirty = true;
        return HandleResult.genHandleSuccess();
    }

    protected HandleResult handleEventCommitNewFee(String[] topics, String data, HandleEventExtraData handleEventExtraData) {
        EventValues eventValues = getEventValue(EVENT_NAME_COMMIT_NEW_FEE, EVENT_NAME_COMMIT_NEW_FEE_BODY, topics, data,
                handleEventExtraData.getUniqueId());
        if (ObjectUtil.isNull(eventValues)) {
            return HandleResult.genHandleFailMessage(String.format("Contract%s, type:%s decode handleEventCommitNewFee fail!, unique id :%s",
                    address, type, handleEventExtraData.getUniqueId()));
        }
        CurveBasePoolData curveBasePoolData = this.getVarCurveBasePoolData();
        BigInteger deadLine = (BigInteger) eventValues.getIndexedValues().get(0).getValue();
        BigInteger fee = (BigInteger) eventValues.getNonIndexedValues().get(0).getValue();
        BigInteger adminFee = (BigInteger) eventValues.getNonIndexedValues().get(1).getValue();
        curveBasePoolData.setFee(fee);
        curveBasePoolData.setAdminFee(adminFee);
        curveBasePoolData.setAdminActionsDeadline(deadLine);
        isDirty = true;
        return HandleResult.genHandleSuccess();
    }

    protected HandleResult handleEventNewFee(String[] topics, String data, HandleEventExtraData handleEventExtraData) {
        EventValues eventValues = getEventValue(EVENT_NAME_NEW_FEE, EVENT_NAME_NEW_FEE_BODY, topics, data,
                handleEventExtraData.getUniqueId());
        if (ObjectUtil.isNull(eventValues)) {
            return HandleResult.genHandleFailMessage(String.format("Contract%s, type:%s decode handleEventNewFee fail!, unique id :%s",
                    address, type, handleEventExtraData.getUniqueId()));
        }
        CurveBasePoolData curveBasePoolData = this.getVarCurveBasePoolData();
        BigInteger fee = (BigInteger) eventValues.getNonIndexedValues().get(0).getValue();
        BigInteger adminFee = (BigInteger) eventValues.getNonIndexedValues().get(1).getValue();
        curveBasePoolData.setFee(fee);
        curveBasePoolData.setAdminFee(adminFee);
        curveBasePoolData.setAdminActionsDeadline(BigInteger.ZERO);
        isDirty = true;
        return HandleResult.genHandleSuccess();
    }

    protected HandleResult handleEventRampA(String[] topics, String data, HandleEventExtraData handleEventExtraData) {
        EventValues eventValues = getEventValue(EVENT_NAME_RAMP_A, EVENT_NAME_RAMP_A_BODY, topics, data,
                handleEventExtraData.getUniqueId());
        if (ObjectUtil.isNull(eventValues)) {
            return HandleResult.genHandleFailMessage(String.format("Contract%s, type:%s decode handleEventRampA fail!, unique id :%s",
                    address, type, handleEventExtraData.getUniqueId()));
        }
        CurveBasePoolData curveBasePoolData = this.getVarCurveBasePoolData();
        BigInteger a = (BigInteger) eventValues.getNonIndexedValues().get(0).getValue();
        BigInteger af = (BigInteger) eventValues.getNonIndexedValues().get(1).getValue();
        BigInteger aT = (BigInteger) eventValues.getNonIndexedValues().get(2).getValue();
        BigInteger afT = (BigInteger) eventValues.getNonIndexedValues().get(3).getValue();
        curveBasePoolData.setInitialA(a);
        curveBasePoolData.setInitialATime(aT);
        curveBasePoolData.setFutureATime(afT);
        curveBasePoolData.setFutureA(af);
        isDirty = true;
        return HandleResult.genHandleSuccess();
    }

    protected HandleResult handleEventStopRampA(String[] topics, String data, HandleEventExtraData handleEventExtraData) {
        EventValues eventValues = getEventValue(EVENT_NAME_STOP_RAMP_A, EVENT_NAME_STOP_RAMP_A_BODY, topics, data,
                handleEventExtraData.getUniqueId());
        if (ObjectUtil.isNull(eventValues)) {
            return HandleResult.genHandleFailMessage(String.format("Contract%s, type:%s decode handleEventStopRampA fail!, unique id :%s",
                    address, type, handleEventExtraData.getUniqueId()));
        }
        CurveBasePoolData curveBasePoolData = this.getVarCurveBasePoolData();
        BigInteger a = (BigInteger) eventValues.getNonIndexedValues().get(0).getValue();
        BigInteger aTime = (BigInteger) eventValues.getNonIndexedValues().get(1).getValue();
        curveBasePoolData.setFutureA(a);
        curveBasePoolData.setInitialA(BigInteger.valueOf(a.longValue()));
        curveBasePoolData.setInitialATime(aTime);
        curveBasePoolData.setInitialATime(BigInteger.valueOf(aTime.longValue()));
        isDirty = true;
        return HandleResult.genHandleSuccess();
    }
}
