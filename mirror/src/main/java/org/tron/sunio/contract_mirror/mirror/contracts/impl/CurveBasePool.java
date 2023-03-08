package org.tron.sunio.contract_mirror.mirror.contracts.impl;

import cn.hutool.core.util.ObjectUtil;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.tron.sunio.contract_mirror.event_decode.events.Curve2PoolEvent;
import org.tron.sunio.contract_mirror.event_decode.events.Curve3PoolEvent;
import org.tron.sunio.contract_mirror.event_decode.utils.GsonUtil;
import org.tron.sunio.contract_mirror.mirror.chainHelper.IChainHelper;
import org.tron.sunio.contract_mirror.mirror.consts.ContractMirrorConst;
import org.tron.sunio.contract_mirror.mirror.contracts.ContractInfo;
import org.tron.sunio.contract_mirror.mirror.contracts.IContractsHelper;
import org.tron.sunio.contract_mirror.mirror.dao.CurveBasePoolData;
import org.tron.sunio.contract_mirror.mirror.enums.ContractType;
import org.tron.sunio.contract_mirror.mirror.tools.CallContractUtil;
import org.tron.sunio.tronsdk.WalletUtil;
import org.web3j.abi.EventValues;
import org.web3j.abi.FunctionEncoder;
import org.web3j.abi.datatypes.StaticArray;
import org.web3j.abi.datatypes.generated.StaticArray2;
import org.web3j.abi.datatypes.generated.StaticArray3;
import org.web3j.abi.datatypes.generated.Uint256;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.tron.sunio.contract_mirror.event_decode.events.Curve2PoolEvent.EVENT_NAME_ADD_LIQUIDITY;
import static org.tron.sunio.contract_mirror.event_decode.events.Curve2PoolEvent.EVENT_NAME_COMMIT_NEW_ADMIN;
import static org.tron.sunio.contract_mirror.event_decode.events.Curve2PoolEvent.EVENT_NAME_COMMIT_NEW_FEE;
import static org.tron.sunio.contract_mirror.event_decode.events.Curve2PoolEvent.EVENT_NAME_NEW_ADMIN;
import static org.tron.sunio.contract_mirror.event_decode.events.Curve2PoolEvent.EVENT_NAME_NEW_FEE;
import static org.tron.sunio.contract_mirror.event_decode.events.Curve2PoolEvent.EVENT_NAME_NEW_FEE_CONVERTER;
import static org.tron.sunio.contract_mirror.event_decode.events.Curve2PoolEvent.EVENT_NAME_RAMP_A;
import static org.tron.sunio.contract_mirror.event_decode.events.Curve2PoolEvent.EVENT_NAME_REMOVE_LIQUIDITY;
import static org.tron.sunio.contract_mirror.event_decode.events.Curve2PoolEvent.EVENT_NAME_REMOVE_LIQUIDITY_IM_BALANCE;
import static org.tron.sunio.contract_mirror.event_decode.events.Curve2PoolEvent.EVENT_NAME_REMOVE_LIQUIDITY_ONE;
import static org.tron.sunio.contract_mirror.event_decode.events.Curve2PoolEvent.EVENT_NAME_STOP_RAMP_A;
import static org.tron.sunio.contract_mirror.event_decode.events.Curve2PoolEvent.EVENT_NAME_TOKEN_EXCHANGE;
import static org.tron.sunio.contract_mirror.mirror.consts.ContractMirrorConst.EMPTY_ADDRESS;
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
public class CurveBasePool extends AbstractCurve {
    @Getter
    protected int coinsCount;
    private static final BigInteger FEE_DENOMINATOR = BigInteger.TEN.pow(10);
    private static final BigInteger LENDING_PRECISION = BigInteger.TEN.pow(18);
    private static final BigInteger PRECISION = BigInteger.TEN.pow(18);
    private static final BigInteger RATES_0 = BigInteger.TEN.pow(18);
    private static final BigInteger RATES_1 = BigInteger.TEN.pow(18);
    private static final BigInteger RATES_2 = BigInteger.TEN.pow(30);
    @Setter
    protected CurveBasePoolData curveBasePoolData;
    private int feeIndex;

    public static CurveBasePool genInstance(ContractInfo contractInfo, IChainHelper iChainHelper, IContractsHelper iContractsHelper,
                                            Map<String, String> sigMap) {
        CurveBasePool.ContractExtraData extraData = CurveBasePool.parseToExtraData(contractInfo.getExtra());
        if (ObjectUtil.isNull(extraData)) {
            return null;
        }
        return new CurveBasePool(contractInfo.getAddress(), contractInfo.getType(), iChainHelper,
                iContractsHelper, extraData.getCoinsCount(), extraData.getFeeIndex(), extraData.getPoolName(), sigMap);
    }


    public CurveBasePool(String address, ContractType type, IChainHelper iChainHelper, IContractsHelper iContractsHelper,
                         int coinsCount, int feeIndex, String poolName, Map<String, String> sigMap) {
        super(address, type, iChainHelper, iContractsHelper, sigMap);
        this.coinsCount = coinsCount;
        this.feeIndex = feeIndex;
        this.poolName = poolName;
    }

    private CurveBasePoolData getVarCurveBasePoolData() {
        if (ObjectUtil.isNull(curveBasePoolData)) {
            curveBasePoolData = new CurveBasePoolData(coinsCount);
            curveBasePoolData.setAddress(address);
            curveBasePoolData.setPoolName(poolName);
            curveBasePoolData.setType(type);
            curveBasePoolData.setUsing(true);
            curveBasePoolData.setReady(false);
            curveBasePoolData.setAddExchangeContracts(false);
        }
        return curveBasePoolData;
    }

    public CurveBasePoolData getCurveBasePoolData() {
        return getVarCurveBasePoolData().copySelf();
    }

    protected void updateCoinsAndBalance(CurveBasePoolData curveBasePoolData) {
        for (int i = 0; i < coinsCount; i++) {
            String coinAddress = CallContractUtil.getTronAddressWithIndex(iChainHelper, ContractMirrorConst.EMPTY_ADDRESS,
                    address, "coins", BigInteger.valueOf(i));
            curveBasePoolData.updateCoins(i, coinAddress);
            if (!coinAddress.equalsIgnoreCase(EMPTY_ADDRESS)) {
                String name = CallContractUtil.getString(iChainHelper, EMPTY_ADDRESS, coinAddress, "name");
                String symbol = CallContractUtil.getString(iChainHelper, EMPTY_ADDRESS, coinAddress, "symbol");
                curveBasePoolData.updateCoinNames(i, name);
                curveBasePoolData.updateCoinSymbols(i, symbol);
            }
            BigInteger balance = CallContractUtil.getU256WithIndex(iChainHelper, ContractMirrorConst.EMPTY_ADDRESS, address,
                    "balances", BigInteger.valueOf(i));
            curveBasePoolData.updateBalances(i, balance);
        }
    }

    protected void updateSupply(CurveBasePoolData curveBasePoolData, String tokenAddress) {
        BigInteger totalSupply = CallContractUtil.getU256(iChainHelper, ContractMirrorConst.EMPTY_ADDRESS, tokenAddress, "totalSupply");
        curveBasePoolData.setTotalSupply(totalSupply);
    }

    @Override
    public boolean initDataFromChain1() {
        CurveBasePoolData curveBasePoolData = this.getVarCurveBasePoolData();
        updateCoinsAndBalance(curveBasePoolData);
        String token = CallContractUtil.getTronAddress(iChainHelper, ContractMirrorConst.EMPTY_ADDRESS, address, "token");
        curveBasePoolData.setToken(token);
        updateSupply(curveBasePoolData, token);
        BigInteger fee = CallContractUtil.getU256(iChainHelper, ContractMirrorConst.EMPTY_ADDRESS, address, "fee");
        curveBasePoolData.setFee(fee);
        BigInteger futureFee = CallContractUtil.getU256(iChainHelper, ContractMirrorConst.EMPTY_ADDRESS, address, "future_fee");
        curveBasePoolData.setFutureFee(futureFee);
        BigInteger adminFee = CallContractUtil.getU256(iChainHelper, ContractMirrorConst.EMPTY_ADDRESS, address, "admin_fee");
        curveBasePoolData.setAdminFee(adminFee);
        BigInteger futureAdminFee = CallContractUtil.getU256(iChainHelper, ContractMirrorConst.EMPTY_ADDRESS, address, "future_admin_fee");
        curveBasePoolData.setFutureAdminFee(futureAdminFee);
        BigInteger adminActionsDeadline = CallContractUtil.getU256(iChainHelper, ContractMirrorConst.EMPTY_ADDRESS, address, "admin_actions_deadline");
        curveBasePoolData.setAdminActionsDeadline(adminActionsDeadline);
        String feeConverter = CallContractUtil.getTronAddress(iChainHelper, ContractMirrorConst.EMPTY_ADDRESS, address, "fee_converter");
        curveBasePoolData.setFeeConverter(feeConverter);
        BigInteger initialA = CallContractUtil.getU256(iChainHelper, ContractMirrorConst.EMPTY_ADDRESS, address, "initial_A");
        curveBasePoolData.setInitialA(initialA);
        BigInteger initialATime = CallContractUtil.getU256(iChainHelper, ContractMirrorConst.EMPTY_ADDRESS, address, "initial_A_time");
        curveBasePoolData.setInitialATime(initialATime);
        BigInteger futureA = CallContractUtil.getU256(iChainHelper, ContractMirrorConst.EMPTY_ADDRESS, address, "future_A");
        curveBasePoolData.setFutureA(futureA);
        BigInteger futureATime = CallContractUtil.getU256(iChainHelper, ContractMirrorConst.EMPTY_ADDRESS, address, "future_A_time");
        curveBasePoolData.setFutureATime(futureATime);
        String owner = CallContractUtil.getTronAddress(iChainHelper, ContractMirrorConst.EMPTY_ADDRESS, address, "owner");
        curveBasePoolData.setOwner(owner);
        String futureOwner = CallContractUtil.getTronAddress(iChainHelper, ContractMirrorConst.EMPTY_ADDRESS, address, "future_owner");
        curveBasePoolData.setFutureOwner(futureOwner);
        BigInteger transferOwnershipDeadline = CallContractUtil.getU256(iChainHelper, ContractMirrorConst.EMPTY_ADDRESS, address, "transfer_ownership_deadline");
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
    public String getVersion() {
        return this.poolName;
    }

    @Override
    public <T> T handleSpecialRequest(String method, String params) throws Exception {
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

    protected HandleResult handleEventTokenExchange(String[] topics, String data, HandleEventExtraData handleEventExtraData) {
        log.info("handleEventTokenExchange: {}, info: {} ", address, type, handleEventExtraData.getUniqueId(), this.getVarCurveBasePoolData());
        CurveBasePoolData curveBasePoolData = this.getVarCurveBasePoolData();
        String body = Curve2PoolEvent.EVENT_NAME_TOKEN_EXCHANGE_BODY;
        if (coinsCount == 3) {
            body = Curve3PoolEvent.EVENT_NAME_TOKEN_EXCHANGE_BODY;
        }
        EventValues eventValues = getEventValue(EVENT_NAME_TOKEN_EXCHANGE, body, topics, data, handleEventExtraData.getUniqueId());
        if (ObjectUtil.isNull(eventValues)) {
            return HandleResult.genHandleFailMessage(String.format("Contract%s, type:%s decode handleEventTokenExchange fail!, unique id :%s",
                    address, type, handleEventExtraData.getUniqueId()));
        }
        int i = ((BigInteger) eventValues.getNonIndexedValues().get(0).getValue()).intValue();
        BigInteger dx = (BigInteger) eventValues.getNonIndexedValues().get(1).getValue();
        int j = ((BigInteger) eventValues.getNonIndexedValues().get(2).getValue()).intValue();
        BigInteger[] rates = getRates();
        BigInteger dy = (BigInteger) eventValues.getNonIndexedValues().get(3).getValue();
        BigInteger tmp = dy.multiply(rates[j]).multiply(FEE_DENOMINATOR);
        BigInteger dyOri = (tmp.divide(FEE_DENOMINATOR.subtract(curveBasePoolData.getFee()))).divide(PRECISION);
        BigInteger dyFee = dyOri.multiply(curveBasePoolData.getFee()).divide(FEE_DENOMINATOR);
        BigInteger dyAdminFee = dyFee.multiply(curveBasePoolData.getAdminFee()).divide(FEE_DENOMINATOR);
        dyAdminFee = dyAdminFee.multiply(PRECISION).divide(rates[j]);
        BigInteger dxWFee = getAmountWFee(i, dx);
        BigInteger newIBalance = curveBasePoolData.getBalances()[i].add(dxWFee);
        BigInteger newJBalance = curveBasePoolData.getBalances()[j].subtract(dy).subtract(dyAdminFee);
        curveBasePoolData.updateBalances(i, newIBalance);
        curveBasePoolData.updateBalances(j, newJBalance);
        this.isDirty = true;
        log.info("{}:{} handleEventTokenExchange finish update data:{}", address, type, handleEventExtraData.getUniqueId());
        return HandleResult.genHandleSuccess();
    }

    private BigInteger getAmountWFee(int _tokenId, BigInteger dx) {
        // 查看CurvePoolV2 feeIndex 是USDT 不是特殊收费ERC20，之后需要在添加
        return dx;
    }


    private BigInteger[] getRates() {
        if (coinsCount == 2) {
            return new BigInteger[]{RATES_0, RATES_2};
        } else {
            return new BigInteger[]{RATES_0, RATES_1, RATES_2};
        }
    }

    private BigInteger[] getPrecisionMul() {
        if (coinsCount == 2) {
            return new BigInteger[]{BigInteger.ONE, BigInteger.TEN.pow(12)};
        } else {
            return new BigInteger[]{BigInteger.ONE, BigInteger.ONE, BigInteger.TEN.pow(12)};
        }
    }


    protected HandleResult handleEventAddLiquidity(String[] topics, String data, HandleEventExtraData handleEventExtraData) {
        log.info("{}:{} handleEventAddLiquidity:{}", address, type, handleEventExtraData.getUniqueId());

        String body = Curve2PoolEvent.EVENT_NAME_ADD_LIQUIDITY_BODY;
        if (coinsCount == 3) {
            body = Curve3PoolEvent.EVENT_NAME_ADD_LIQUIDITY_BODY;
        }
        EventValues eventValues = getEventValue(EVENT_NAME_ADD_LIQUIDITY, body, topics, data,
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
            BigInteger amountWFee = getAmountWFee(i, (BigInteger) amounts.getValue().get(i).getValue());
            BigInteger originBalance = curveBasePoolData.getBalances()[i];
            BigInteger newBalance = originBalance.add(amountWFee);
            if (curveBasePoolData.getTotalSupply().compareTo(BigInteger.ZERO) > 0) {
                BigInteger fee = fees.getValue().get(i).getValue();
                BigInteger newFee = fee.multiply(curveBasePoolData.getAdminFee()).divide(FEE_DENOMINATOR);
                newBalance = newBalance.subtract(newFee);
            }
            curveBasePoolData.updateBalances(i, newBalance);
            amountsNew.add(new Uint256(newBalance));
        }
        BigInteger newTotalSupply = (BigInteger) eventValues.getNonIndexedValues().get(3).getValue();
        curveBasePoolData.setTotalSupply(newTotalSupply);
        this.isDirty = true;
        log.info("{}:{} handleEventAddLiquidity finish update data:{}", address, type, handleEventExtraData.getUniqueId());
        StaticArray<Uint256> amountsStatic;
        if (coinsCount == 2) {
            amountsStatic = new StaticArray2(Uint256.class, amountsNew);
        } else {
            amountsStatic = new StaticArray3(Uint256.class, amountsNew);
        }
        String newData = FunctionEncoder.encodeConstructor(
                Arrays.asList(
                        amountsStatic,
                        eventValues.getNonIndexedValues().get(1),
                        eventValues.getNonIndexedValues().get(2),
                        eventValues.getNonIndexedValues().get(3)
                )
        );
        log.info("{}:{} handleEventAddLiquidity finish gen new log:{}", address, type, handleEventExtraData.getUniqueId());

        return HandleResult.genHandleSuccessAndSend(topics, newData);
    }

    protected HandleResult handleEventRemoveLiquidity(String[] topics, String data, HandleEventExtraData handleEventExtraData) {
        log.info("{}:{} handleEventRemoveLiquidity:{}", address, type, handleEventExtraData.getUniqueId());
        String body = Curve2PoolEvent.EVENT_NAME_REMOVE_LIQUIDITY_BODY;
        if (coinsCount == 3) {
            body = Curve3PoolEvent.EVENT_NAME_REMOVE_LIQUIDITY_BODY;
        }
        EventValues eventValues = getEventValue(EVENT_NAME_REMOVE_LIQUIDITY, body, topics, data,
                handleEventExtraData.getUniqueId());
        if (ObjectUtil.isNull(eventValues)) {
            return HandleResult.genHandleFailMessage(String.format("Contract%s, type:%s decode handleEventRemoveLiquidity fail!, unique id :%s",
                    address, type, handleEventExtraData.getUniqueId()));
        }
        CurveBasePoolData curveBasePoolData = this.getVarCurveBasePoolData();
        StaticArray<Uint256> amounts = (StaticArray<Uint256>) eventValues.getNonIndexedValues().get(0);
        List<Uint256> amountsNew = new ArrayList<>();
        for (int i = 0; i < coinsCount; i++) {
            BigInteger origin = curveBasePoolData.getBalances()[i];
            BigInteger newBalance = origin.subtract((BigInteger) amounts.getValue().get(i).getValue());
            curveBasePoolData.updateBalances(i, newBalance);
            amountsNew.add(new Uint256(newBalance));
        }
        BigInteger newTotalSupply = (BigInteger) eventValues.getNonIndexedValues().get(2).getValue();
        curveBasePoolData.setTotalSupply(newTotalSupply);
        this.isDirty = true;
        log.info("{}:{} handleEventRemoveLiquidity finish update data:{}", address, type, handleEventExtraData.getUniqueId());
        StaticArray<Uint256> amountsStatic;
        if (coinsCount == 2) {
            amountsStatic = new StaticArray2(Uint256.class, amountsNew);
        } else {
            amountsStatic = new StaticArray3(Uint256.class, amountsNew);
        }
        String newData = FunctionEncoder.encodeConstructor(
                Arrays.asList(
                        amountsStatic,
                        eventValues.getNonIndexedValues().get(1),
                        eventValues.getNonIndexedValues().get(2)
                )
        );
        log.info("{}:{} handleEventRemoveLiquidity finish gen event log:{}", address, type, handleEventExtraData.getUniqueId());
        return HandleResult.genHandleSuccessAndSend(topics, newData);
    }

    protected HandleResult handleEventRemoveLiquidityOne(String[] topics, String data, HandleEventExtraData handleEventExtraData) {
        log.info("{}:{} handleEventRemoveLiquidityOne:{}", address, type, handleEventExtraData.getUniqueId());
        updateBaseInfo(isUsing, false, isAddExchangeContracts);
        this.isReady = false;
        this.isDirty = true;
        return HandleResult.genHandleSuccess();
    }

    protected HandleResult handleEventRemoveLiquidityImbalance(String[] topics, String data, HandleEventExtraData handleEventExtraData) {
        log.info("{}:{} handleEventRemoveLiquidityImbalance:{}", address, type, handleEventExtraData.getUniqueId());
        String body = Curve2PoolEvent.EVENT_NAME_REMOVE_LIQUIDITY_IM_BALANCE_BODY;
        if (coinsCount == 3) {
            body = Curve3PoolEvent.EVENT_NAME_REMOVE_LIQUIDITY_IM_BALANCE_BODY;
        }
        EventValues eventValues = getEventValue(EVENT_NAME_REMOVE_LIQUIDITY_IM_BALANCE, body,
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
            BigInteger originBalance = curveBasePoolData.getBalances()[i];
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
        log.info("{}:{} handleEventRemoveLiquidityImbalance finish update data:{}", address, type, handleEventExtraData.getUniqueId());
        StaticArray<Uint256> amountsStatic;
        if (coinsCount == 2) {
            amountsStatic = new StaticArray2(Uint256.class, amountsNew);
        } else {
            amountsStatic = new StaticArray3(Uint256.class, amountsNew);
        }
        String newData = FunctionEncoder.encodeConstructor(
                Arrays.asList(
                        amountsStatic,
                        eventValues.getNonIndexedValues().get(1),
                        eventValues.getNonIndexedValues().get(2),
                        eventValues.getNonIndexedValues().get(3)
                )
        );
        log.info("{}:{} handleEventRemoveLiquidityImbalance finish gen event log:{}", address, type, handleEventExtraData.getUniqueId());
        return HandleResult.genHandleSuccessAndSend(topics, newData);
    }

    protected HandleResult handleEventCommitNewAdmin(String[] topics, String data, HandleEventExtraData handleEventExtraData) {
        log.info("{}:{} handleEventCommitNewAdmin:{}", address, type, handleEventExtraData.getUniqueId());
        String body = Curve2PoolEvent.EVENT_NAME_COMMIT_NEW_ADMIN_BODY;
        if (coinsCount == 3) {
            body = Curve3PoolEvent.EVENT_NAME_COMMIT_NEW_ADMIN_BODY;
        }
        EventValues eventValues = getEventValue(EVENT_NAME_COMMIT_NEW_ADMIN, body, topics, data,
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
        log.info("{}:{} handleEventCommitNewAdmin finish update data:{}", address, type, handleEventExtraData.getUniqueId());
        return HandleResult.genHandleSuccess();
    }

    protected HandleResult handleEventNewAdmin(String[] topics, String data, HandleEventExtraData handleEventExtraData) {
        log.info("{}:{} handleEventNewAdmin:{}", address, type, handleEventExtraData.getUniqueId());
        String body = Curve2PoolEvent.EVENT_NAME_NEW_ADMIN_BODY;
        if (coinsCount == 3) {
            body = Curve3PoolEvent.EVENT_NAME_NEW_ADMIN_BODY;
        }
        EventValues eventValues = getEventValue(EVENT_NAME_NEW_ADMIN, body, topics, data,
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
        log.info("{}:{} handleEventNewAdmin finish update data:{}", address, type, handleEventExtraData.getUniqueId());
        return HandleResult.genHandleSuccess();
    }

    protected HandleResult handleEventNewFeeConverter(String[] topics, String data, HandleEventExtraData handleEventExtraData) {
        log.info("{}:{} handleEventNewFeeConverter:{}", address, type, handleEventExtraData.getUniqueId());
        String body = Curve2PoolEvent.EVENT_NAME_NEW_FEE_CONVERTER_BODY;
        if (coinsCount == 3) {
            body = Curve3PoolEvent.EVENT_NAME_NEW_FEE_CONVERTER_BODY;
        }
        EventValues eventValues = getEventValue(EVENT_NAME_NEW_FEE_CONVERTER, body, topics, data,
                handleEventExtraData.getUniqueId());
        if (ObjectUtil.isNull(eventValues)) {
            return HandleResult.genHandleFailMessage(String.format("Contract%s, type:%s decode handleEventNewFeeConverter fail!, unique id :%s",
                    address, type, handleEventExtraData.getUniqueId()));
        }
        String feeConverter = WalletUtil.hexStringToTron((String) eventValues.getIndexedValues().get(0).getValue());
        CurveBasePoolData curveBasePoolData = this.getVarCurveBasePoolData();
        curveBasePoolData.setFeeConverter(feeConverter);
        isDirty = true;
        log.info("{}:{} handleEventNewFeeConverter finish update data:{}", address, type, handleEventExtraData.getUniqueId());
        return HandleResult.genHandleSuccess();
    }

    protected HandleResult handleEventCommitNewFee(String[] topics, String data, HandleEventExtraData handleEventExtraData) {
        log.info("{}:{} handleEventCommitNewFee:{}", address, type, handleEventExtraData.getUniqueId());
        String body = Curve2PoolEvent.EVENT_NAME_COMMIT_NEW_FEE_BODY;
        if (coinsCount == 3) {
            body = Curve3PoolEvent.EVENT_NAME_COMMIT_NEW_FEE_BODY;
        }
        EventValues eventValues = getEventValue(EVENT_NAME_COMMIT_NEW_FEE, body, topics, data,
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
        log.info("{}:{} handleEventCommitNewFee finish update data:{}", address, type, handleEventExtraData.getUniqueId());
        return HandleResult.genHandleSuccess();
    }

    protected HandleResult handleEventNewFee(String[] topics, String data, HandleEventExtraData handleEventExtraData) {
        log.info("{}:{} handleEventNewFee:{}", address, type, handleEventExtraData.getUniqueId());
        String body = Curve2PoolEvent.EVENT_NAME_NEW_FEE_BODY;
        if (coinsCount == 3) {
            body = Curve3PoolEvent.EVENT_NAME_NEW_FEE_BODY;
        }
        EventValues eventValues = getEventValue(EVENT_NAME_NEW_FEE, body, topics, data,
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
        log.info("{}:{} handleEventNewFee finish update data:{}", address, type, handleEventExtraData.getUniqueId());
        return HandleResult.genHandleSuccess();
    }

    protected HandleResult handleEventRampA(String[] topics, String data, HandleEventExtraData handleEventExtraData) {
        log.info("{}:{} handleEventRampA:{}", address, type, handleEventExtraData.getUniqueId());
        String body = Curve2PoolEvent.EVENT_NAME_RAMP_A_BODY;
        if (coinsCount == 3) {
            body = Curve3PoolEvent.EVENT_NAME_RAMP_A_BODY;
        }
        EventValues eventValues = getEventValue(EVENT_NAME_RAMP_A, body, topics, data,
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
        log.info("{}:{} handleEventRampA finish update data:{}", address, type, handleEventExtraData.getUniqueId());
        return HandleResult.genHandleSuccess();
    }

    protected HandleResult handleEventStopRampA(String[] topics, String data, HandleEventExtraData handleEventExtraData) {
        log.info("{}:{} handleEventStopRampA:{}", address, type, handleEventExtraData.getUniqueId());
        String body = Curve2PoolEvent.EVENT_NAME_STOP_RAMP_A_BODY;
        if (coinsCount == 3) {
            body = Curve3PoolEvent.EVENT_NAME_STOP_RAMP_A_BODY;
        }
        EventValues eventValues = getEventValue(EVENT_NAME_STOP_RAMP_A, body, topics, data,
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
        log.info("{}:{} handleEventStopRampA finish update data:{}", address, type, handleEventExtraData.getUniqueId());
        return HandleResult.genHandleSuccess();
    }

    private BigInteger[] xp() {
        BigInteger[] result = getRates();
        BigInteger[] balances = this.getVarCurveBasePoolData().getBalances();
        for (int i = 0; i < coinsCount; i++) {
            result[i] = result[i].multiply(balances[i]).divide(LENDING_PRECISION);
        }
        return result;
    }

    private BigInteger[] xpMem(BigInteger[] balances) {
        BigInteger[] result = getRates();
        for (int i = 0; i < coinsCount; i++) {
            result[i] = result[i].multiply(balances[i]).divide(PRECISION);
        }
        return result;
    }

    protected BigInteger getD(BigInteger[] xp, BigInteger amp) throws Exception {
        if (xp == null || xp.length != coinsCount) {
            throw new Exception("N_COINS not eq xp.length");
        }
        BigInteger s = BigInteger.ZERO;
        for (int j = 0; j < coinsCount; j++) {
            s = s.add(xp[j]);
        }

        if (s.compareTo(BigInteger.ZERO) == 0) {
            return BigInteger.ZERO;
        }
        BigInteger dPre;
        BigInteger d = new BigInteger(s.toString());
        BigInteger ann = amp.multiply(BigInteger.valueOf(coinsCount));
        for (int i = 0; i < 255; i++) {
            BigInteger dp = d;
            for (int k = 0; k < coinsCount; k++) {
                dp = dp.multiply(d).divide(xp[k].multiply(BigInteger.valueOf(coinsCount)));
            }
            dPre = d;
            BigInteger tmp = (ann.multiply(s)).add(dp.multiply(BigInteger.valueOf(coinsCount))).multiply(d);
            d = tmp.divide(ann.subtract(BigInteger.ONE).multiply(d).add(BigInteger.valueOf(coinsCount + 1).multiply(dp)));
            if (d.compareTo(dPre) > 0) {
                if (d.subtract(dPre).compareTo(BigInteger.ONE) <= 0) {
                    break;
                }
            } else {
                if (dPre.subtract(d).compareTo(BigInteger.ONE) <= 0) {
                    break;
                }
            }

        }
        return d;
    }

    public BigInteger a(long blocTime) {
        CurveBasePoolData curveBasePoolData = this.getVarCurveBasePoolData();
        BigInteger t1 = curveBasePoolData.getFutureATime();
        BigInteger a1 = curveBasePoolData.getFutureA();
        if (blocTime < t1.longValue()) {
            BigInteger a0 = curveBasePoolData.getInitialA();
            BigInteger t0 = curveBasePoolData.getInitialATime();
            BigInteger tCast = BigInteger.valueOf(blocTime).subtract(t0);
            BigInteger tCastMax = t1.subtract(t0);
            if (a1.compareTo(a0) > 0) {
                return a0.add(a1.subtract(a0).multiply(tCast).divide(tCastMax));
            } else {
                return a0.subtract(a0.subtract(a1).multiply(tCast).divide(tCastMax));
            }

        } else {
            return a1;
        }
    }

    protected BigInteger getDMem(BigInteger[] balances, BigInteger amp) throws Exception {
        return getD(xpMem(balances), amp);
    }

    @Override
    public String coins(int i) {
        return null;
    }

    @Override
    public BigInteger getVirtualPrice(long timestamp) throws Exception {
        BigInteger d = getD(xp(), a(timestamp));
        BigInteger totalSupply = getVarCurveBasePoolData().getTotalSupply();
        return d.multiply(PRECISION).divide(totalSupply);
    }

    @Override
    public BigInteger calcTokenAmount(long timestamp, BigInteger[] amounts, boolean deposit) throws Exception {
        BigInteger[] balances = getVarCurveBasePoolData().getCopyBalances();
        BigInteger amp = a(timestamp);
        BigInteger d0 = getDMem(balances, amp);
        for (int i = 0; i < coinsCount; i++) {
            if (deposit) {
                balances[i] = balances[i].add(amounts[i]);
            } else {
                balances[i] = balances[i].subtract(amounts[i]);
            }
        }
        BigInteger d1 = getDMem(balances, amp);
        BigInteger tokenAmount = getVarCurveBasePoolData().getTotalSupply();
        BigInteger diff;
        if (deposit) {
            diff = d1.subtract(d0);
        } else {
            diff = d0.subtract(d1);
        }
        return diff.multiply(tokenAmount).divide(d0);
    }

    public BigInteger getY(int i, int j, BigInteger x, BigInteger[] xp_, long timestamp) throws Exception {
        if (i == j) {
            throw new Exception("same coin");
        }
        if (j >= coinsCount) {
            throw new Exception("j above N_COINS");
        }

        if (i >= coinsCount) {
            throw new Exception("i above N_COINS");
        }
        BigInteger amp = a(timestamp);
        BigInteger d = getD(xp_, amp);
        BigInteger c = d;
        BigInteger s_ = BigInteger.ZERO;
        BigInteger nCoins = BigInteger.valueOf(coinsCount);
        BigInteger ann = amp.multiply(nCoins);
        BigInteger _x = BigInteger.ZERO;
        for (int _i = 0; _i < coinsCount; _i++) {
            if (_i == i) {
                _x = x;
            } else if (_i != j) {
                _x = xp_[_i];
            } else {
                continue;
            }
            s_ = s_.add(_x);
            c = c.multiply(d).divide(_x.multiply(nCoins));
        }
        c = c.multiply(d).divide(ann.multiply(nCoins));
        BigInteger b = s_.add(d.divide(ann));
        BigInteger yPrev = BigInteger.ZERO;
        BigInteger y = d;
        for (int _i = 0; _i < 255; _i++) {
            yPrev = y;
            y = (y.multiply(y).add(c)).divide(y.multiply(BigInteger.TWO).add(b).subtract(d));
            if (y.compareTo(yPrev) > 0) {
                if (y.subtract(yPrev).compareTo(BigInteger.ONE) <= 0) {
                    break;
                }
            } else {
                if (yPrev.subtract(y).compareTo(BigInteger.ONE) <= 0) {
                    break;
                }
            }
        }
        return y;
    }

    public BigInteger getDy(int i, int j, BigInteger dx, long timestamp) throws Exception {
        BigInteger[] rates = getRates();
        BigInteger[] xp = xp();
        BigInteger fee = getVarCurveBasePoolData().getFee();
        BigInteger x = xp[i].add(dx.multiply(rates[i]).divide(PRECISION));
        BigInteger y = getY(i, j, x, xp, timestamp);
        BigInteger dy = (xp[j].subtract(y).subtract(BigInteger.ONE)).multiply(PRECISION).divide(rates[j]);
        return dy.subtract(fee.multiply(dy).divide(FEE_DENOMINATOR));
    }

    public BigInteger getDyUnderLying(int i, int j, BigInteger dx, long timestamp) throws Exception {
        BigInteger[] precisions = getPrecisionMul();
        BigInteger[] xp = xp();
        BigInteger fee = getVarCurveBasePoolData().getFee();
        BigInteger x = xp[i].add(dx.multiply(precisions[i]));
        BigInteger y = getY(i, j, x, xp, timestamp);
        BigInteger dy = (xp[j].subtract(y).subtract(BigInteger.ONE)).divide(precisions[j]);
        return dy.subtract(fee.multiply(dy).divide(FEE_DENOMINATOR));
    }

    public BigInteger getYD(BigInteger a_, int i, BigInteger[] xp, BigInteger d) throws Exception {
        if (i >= coinsCount) {
            throw new Exception("i above N_COINS");
        }
        BigInteger c = d;
        BigInteger s_ = BigInteger.ZERO;
        BigInteger nCoins = BigInteger.valueOf(coinsCount);
        BigInteger ann = a_.multiply(nCoins);
        BigInteger _x = BigInteger.ZERO;
        for (int _i = 0; _i < coinsCount; _i++) {
            if (_i != i) {
                _x = xp[_i];
            } else {
                continue;
            }
            s_ = s_.add(_x);
            c = c.multiply(d).divide(_x.multiply(nCoins));
        }
        c = c.multiply(d).divide(ann.multiply(nCoins));
        BigInteger b = s_.add(d.divide(ann));

        BigInteger yPrev = BigInteger.ZERO;
        BigInteger y = d;

        for (int _i = 0; _i < 255; _i++) {
            yPrev = y;
            y = (y.multiply(y).add(c)).divide(y.multiply(BigInteger.TWO).add(b).subtract(d));
            if (y.compareTo(yPrev) > 0) {
                if (y.subtract(yPrev).compareTo(BigInteger.ONE) <= 0) {
                    break;
                }
            } else {
                if (yPrev.subtract(y).compareTo(BigInteger.ONE) <= 0) {
                    break;
                }
            }
        }

        return y;
    }

    private BigInteger[] localCalcWithdrawOneCoin(BigInteger _token_amount, int i, long timestamp, CurveBasePoolData curveBasePoolData) throws Exception {
        BigInteger nCoins = BigInteger.valueOf(coinsCount);
        BigInteger[] precisions = getPrecisionMul();
        BigInteger amp = a(timestamp);
        BigInteger _fee = curveBasePoolData.getFee().multiply(nCoins).divide(BigInteger.valueOf((coinsCount - 1) * 4));
        BigInteger totalSupply = curveBasePoolData.getTotalSupply();

        BigInteger[] xp = xp();
        BigInteger d0 = getD(xp, amp);
        BigInteger d1 = d0.subtract(_token_amount.multiply(d0).divide(totalSupply));
        BigInteger[] xpReduced = xp();
        BigInteger newY = getYD(amp, i, xp, d1);
        BigInteger dy0 = (xp[i].subtract(newY)).divide(precisions[i]);  // w/o fees

        for (int j = 0; j < coinsCount; j++) {
            BigInteger dxExpected = BigInteger.ZERO;
            if (j == i) {
                dxExpected = xp[j].multiply(d1).divide(d0).subtract(newY);
            } else {
                dxExpected = xp[j].subtract(xp[j].multiply(d1).divide(d0));
            }
            xpReduced[j] = xpReduced[j].subtract(_fee.multiply(dxExpected).divide(FEE_DENOMINATOR));
        }

        BigInteger dy = xpReduced[i].subtract(getYD(amp, i, xpReduced, d1));
        dy = (dy.subtract(BigInteger.ONE)).divide(precisions[i]);
        return new BigInteger[]{dy, dy0.subtract(dy)};
    }

    public BigInteger calcWithdrawOneCoin(long timestamp, BigInteger _token_amount, int i) throws Exception {
        BigInteger[] res = localCalcWithdrawOneCoin(_token_amount, i, timestamp, this.getVarCurveBasePoolData());
        return res[0];
    }

    @Override
    public BigInteger fee() {
        return this.getVarCurveBasePoolData().getFee();
    }

    @Override
    public BigInteger adminFee() {
        return this.getVarCurveBasePoolData().getAdminFee();
    }

    @Override
    public BigInteger[] rates(long timestamp) {
        return getRates();
    }

    @Override
    public BigInteger getDyUnderlying(int i, int j, BigInteger dx, long timestamp) throws Exception {
        BigInteger[] xp = xp();
        BigInteger[] precisions = getPrecisionMul();
        BigInteger x = xp[i].add(dx.multiply(precisions[i]));
        BigInteger y = getY(i, j, x, xp, timestamp);
        BigInteger dy = (xp[j].subtract(y).subtract(BigInteger.ONE)).divide(precisions[j]);
        BigInteger _fee = fee().multiply(dy).divide(FEE_DENOMINATOR);
        return dy.subtract(_fee);
    }

    public BigInteger exchange(int i, int j, BigInteger dx, BigInteger min_dy, long timestamp) throws Exception {
        CurveBasePoolData poolData = this.getVarCurveBasePoolData();
        return exchange(i, j, dx, min_dy, timestamp, poolData);
    }

    @Override
    public BigInteger exchangeUnderlying(int i, int j, BigInteger _dx, BigInteger mindy, long timestamp) throws Exception {
        CurveBasePoolData poolData = this.getVarCurveBasePoolData();
        return exchange(i, j, _dx, mindy, timestamp, poolData);
    }

    @Override
    public AbstractCurve copySelf() {
        CurveBasePoolData poolData = this.getCurveBasePoolData();
        CurveBasePool pool = new CurveBasePool(
                address,
                type,
                iChainHelper,
                iContractsHelper,
                coinsCount,
                feeIndex,
                poolName,
                sigMap
        );
        pool.setCurveBasePoolData(poolData);
        return pool;
    }

    @Override
    public double calcFee(long timestamp, int j) {
        BigInteger[] rates = this.rates(timestamp);
        BigDecimal feeV = new BigDecimal(this.getVarCurveBasePoolData().getFee());
        BigDecimal adminFeeV = new BigDecimal(this.getVarCurveBasePoolData().getAdminFee());
        BigDecimal fee = feeV.divide(new BigDecimal(FEE_DENOMINATOR), 6, RoundingMode.UP);
        BigDecimal feeAdmin = fee.multiply(adminFeeV).multiply(new BigDecimal(PRECISION))
                .divide(new BigDecimal(FEE_DENOMINATOR), 6, RoundingMode.UP).divide(new BigDecimal(rates[j]), 6, RoundingMode.UP);
        return fee.add(feeAdmin).doubleValue();
    }

    @Override
    public double calcBasePoolFee(long timestamp, int j) {
        return 0;
    }

    public BigInteger exchange(int i, int j, BigInteger dx, BigInteger min_dy, long timestamp, CurveBasePoolData poolData) throws Exception {
        BigInteger[] rates = getRates();
        BigInteger[] oldBalances = poolData.copyBalances();
        BigInteger[] xp = xpMem(oldBalances);
        BigInteger dxWFee = getAmountWFee(i, dx);
        BigInteger x = xp[i].add(dxWFee.multiply(rates[i]).divide(PRECISION));
        BigInteger y = getY(i, j, x, xp, timestamp);
        BigInteger dy = xp[j].subtract(y).subtract(BigInteger.ONE);
        BigInteger dyFee = dy.multiply(poolData.getFee()).divide(FEE_DENOMINATOR);
        dy = (dy.subtract(dyFee)).multiply(PRECISION).divide(rates[j]);
        if (dy.compareTo(min_dy) < 0) {
            throw new Exception("Exchange resulted in fewer coins than expected");
        }
        BigInteger dyAminFee = dyFee.multiply(poolData.getAdminFee()).divide(FEE_DENOMINATOR);
        dyAminFee = dyAminFee.multiply(PRECISION).divide(rates[j]);

        poolData.updateBalances(i, oldBalances[i].add(dxWFee));
        poolData.updateBalances(j, oldBalances[j].subtract(dy).subtract(dyAminFee));
        return dy;
    }


    public BigInteger[] removeLiquidity(BigInteger _amount, BigInteger[] _minAmounts, long timestamp) throws Exception {
        return removeLiquidity(_amount, _minAmounts);
    }

    @Override
    public BigInteger removeLiquidityImBalance(BigInteger[] _amounts, BigInteger _minBurnAmount, long timestamp) throws Exception {
        return removeLiquidityImbalance(_amounts, _minBurnAmount, timestamp);
    }

    @Override
    public BigInteger addLiquidity(BigInteger[] amounts, BigInteger minMintAmount, long timestamp) throws Exception {
        CurveBasePoolData poolData = this.getVarCurveBasePoolData();
        return addLiquidity(amounts, minMintAmount, timestamp, poolData);
    }

    public BigInteger addLiquidity(BigInteger[] amounts, BigInteger minMintAmount,
                                   long timestamp, CurveBasePoolData poolData) throws Exception {
        BigInteger amp = a(timestamp);
        BigInteger tokenSupply = poolData.getTotalSupply();
        // Initial invariant
        BigInteger d0 = BigInteger.ZERO;
        BigInteger[] oldBalances = poolData.copyBalances();
        if (tokenSupply.compareTo(BigInteger.ZERO) > 0) {
            d0 = getDMem(oldBalances, amp);
        }
        BigInteger[] newBalances = new BigInteger[coinsCount];

        for (int i = 0; i < coinsCount; i++) {
            BigInteger inAmount = amounts[i];
            if (tokenSupply.compareTo(BigInteger.ZERO) <= 0) {
                throw new Exception("in_amount must gt 0");
            }
            inAmount = getAmountWFee(i, amounts[i]);
            newBalances[i] = oldBalances[i].add(inAmount);
        }


        BigInteger d1 = getDMem(newBalances, amp);
        if (d1.compareTo(d0) <= 0) {
            throw new Exception("D1 must gt D0");
        }
        BigInteger mintAmount;
        BigInteger[] fees = new BigInteger[coinsCount];
        BigInteger d2 = d1;

        if (tokenSupply.compareTo(BigInteger.ZERO) > 0) {
            BigInteger _fee = poolData.getFee().multiply(BigInteger.valueOf(coinsCount)).divide(BigInteger.valueOf((coinsCount - 1) * 4));
            BigInteger _admin_fee = poolData.getAdminFee();
            for (int i = 0; i < coinsCount; i++) {
                BigInteger idealBalance = d1.multiply(oldBalances[i]).divide(d0);
                BigInteger difference = BigInteger.ZERO;

                if (idealBalance.compareTo(newBalances[i]) > 0) {
                    difference = idealBalance.subtract(newBalances[i]);
                } else {
                    difference = newBalances[i].subtract(idealBalance);
                }
                fees[i] = _fee.multiply(difference).divide(FEE_DENOMINATOR);
                poolData.getBalances()[i] = newBalances[i].subtract(fees[i].multiply(_admin_fee).divide(FEE_DENOMINATOR));
                newBalances[i] = newBalances[i].subtract(fees[i]);
            }

            d2 = getDMem(newBalances, amp);
        } else {
            poolData.setBalances(newBalances);
        }
        if (tokenSupply.compareTo(BigInteger.ZERO) == 0) {
            mintAmount = d1;
        } else {
            mintAmount = tokenSupply.multiply(d2.subtract(d0)).divide(d0);
        }

        if (mintAmount.compareTo(minMintAmount) < 0) {
            throw new Exception("Slippage screwed you");
        }
        poolData.setTotalSupply(poolData.getTotalSupply().add(mintAmount));
        return mintAmount;
    }

    public BigInteger[] removeLiquidity(BigInteger _amount, BigInteger[] minAmounts) throws Exception {
        CurveBasePoolData poolData = this.getVarCurveBasePoolData();
        return removeLiquidity(_amount, minAmounts, poolData);
    }

    public BigInteger[] removeLiquidity(BigInteger _amount, BigInteger[] minAmounts,
                                        CurveBasePoolData poolData) throws Exception {
        BigInteger tokenSupply = poolData.getTotalSupply();
        BigInteger[] amounts = new BigInteger[coinsCount];
        BigInteger[] fees = new BigInteger[coinsCount];
        for (int i = 0; i < coinsCount; i++) {
            BigInteger value = poolData.getBalances()[i].multiply(_amount).divide(tokenSupply);
            if (value.compareTo(minAmounts[i]) < 0) {
                throw new Exception("Withdrawal resulted in fewer coins than expected");
            }
            amounts[i] = value;
            poolData.getBalances()[i] = poolData.getBalances()[i].subtract(value);
        }
        poolData.setTotalSupply(poolData.getTotalSupply().subtract(_amount));
        return amounts;
    }


    public BigInteger removeLiquidityOneCoin(BigInteger _token_amount, int i, BigInteger min_amount, long timestamp) throws Exception {
        CurveBasePoolData poolData = this.getVarCurveBasePoolData();
        return removeLiquidityOneCoin(_token_amount, i, min_amount, timestamp, poolData);
    }

    public BigInteger removeLiquidityOneCoin(BigInteger _token_amount, int i, BigInteger min_amount, long timestamp,
                                             CurveBasePoolData poolData) throws Exception {
        BigInteger[] calRes = localCalcWithdrawOneCoin(_token_amount, i, timestamp, poolData);
        if (calRes[0].compareTo(min_amount) < 0) {
            new Exception("Not enough coins removed");
        }
        poolData.getBalances()[i] = poolData.getBalances()[i].subtract(calRes[0].add(calRes[1].multiply(
                poolData.getAdminFee()).divide(FEE_DENOMINATOR)));
        poolData.setTotalSupply(poolData.getTotalSupply().subtract(_token_amount));
        return calRes[0];
    }

    public BigInteger removeLiquidityImbalance(BigInteger[] amounts, BigInteger max_burn_amount, long timestamp) throws Exception {
        CurveBasePoolData poolData = this.getVarCurveBasePoolData();
        return removeLiquidityImbalance(amounts, max_burn_amount, timestamp, poolData);
    }

    public BigInteger removeLiquidityImbalance(BigInteger[] amounts, BigInteger max_burn_amount, long timestamp,
                                               CurveBasePoolData poolData) throws Exception {
        BigInteger tokenSupply = poolData.getTotalSupply();
        if (tokenSupply.compareTo(BigInteger.ZERO) == 0) {
            throw new Exception("zero total supply");
        }
        BigInteger _fee = poolData.getFee().multiply(BigInteger.valueOf(coinsCount))
                .divide(BigInteger.valueOf((coinsCount - 1) * 4));

        BigInteger _admin_fee = poolData.getAdminFee();
        BigInteger amp = a(timestamp);
        BigInteger[] old_balances = poolData.getBalances();
        BigInteger[] new_balances = poolData.copyBalances();
        BigInteger D0 = getDMem(old_balances, amp);

        for (int i = 0; i < coinsCount; i++) {
            new_balances[i] = new_balances[i].subtract(amounts[i]);
        }
        BigInteger D1 = getDMem(new_balances, amp);
        // uint256[] memory fees =  new uint256[](N_COINS);
        BigInteger[] fees = new BigInteger[coinsCount];

        for (int i = 0; i < coinsCount; i++) {
            BigInteger ideal_balance = D1.multiply(old_balances[i]).divide(D0);
            BigInteger difference;
            if (ideal_balance.compareTo(new_balances[i]) > 0) {
                difference = ideal_balance.subtract(new_balances[i]);
            } else {
                difference = new_balances[i].subtract(ideal_balance);
            }
            fees[i] = _fee.multiply(difference).divide(FEE_DENOMINATOR);
            poolData.getBalances()[i] = new_balances[i].subtract((fees[i].multiply(_admin_fee).divide(FEE_DENOMINATOR)));
            new_balances[i] = new_balances[i].subtract(fees[i]);
        }
        BigInteger token_amount;
        {
            BigInteger D2 = getDMem(new_balances, amp);
            token_amount = (D0.subtract(D2)).multiply(tokenSupply).divide(D0);
            if (token_amount.compareTo(BigInteger.ZERO) == 0) {
                throw new Exception("zero tokens burned");
            }

            token_amount = token_amount.add(BigInteger.ONE);
        }
        if (token_amount.compareTo(max_burn_amount) > 0) {
            throw new Exception("Slippage screwed you");
        }

        poolData.setTotalSupply(tokenSupply.subtract(token_amount));
        return token_amount;
    }


    @Override
    public String toString() {
        return "CurveBasePool{" +
                "coinsCount=" + coinsCount +
                ", curveBasePoolData=" + curveBasePoolData +
                ", feeIndex=" + feeIndex +
                '}';
    }

    @Data
    public static class ContractExtraData {
        private int coinsCount;
        private int feeIndex;
        private String poolName;
    }

    public static CurveBasePool.ContractExtraData parseToExtraData(String input) {
        try {
            return GsonUtil.gsonToObject(input, CurveBasePool.ContractExtraData.class);
        } catch (Exception e) {
            log.error("Parse psm failed, input:{} err:{}", e);
        }
        return null;
    }
}
