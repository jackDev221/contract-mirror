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
import org.tron.sunio.contract_mirror.mirror.dao.StableSwapPoolData;
import org.tron.sunio.contract_mirror.mirror.enums.ContractType;
import org.tron.sunio.contract_mirror.mirror.tools.CallContractUtil;
import org.tron.sunio.tronsdk.WalletUtil;
import org.web3j.abi.EventValues;
import org.web3j.abi.FunctionEncoder;
import org.web3j.abi.datatypes.StaticArray;
import org.web3j.abi.datatypes.generated.StaticArray2;
import org.web3j.abi.datatypes.generated.StaticArray3;
import org.web3j.abi.datatypes.generated.Uint256;

import java.math.BigInteger;
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
import static org.tron.sunio.contract_mirror.event_decode.events.Curve2PoolEvent.EVENT_NAME_TOKEN_EXCHANGE_UNDERLING;
import static org.tron.sunio.contract_mirror.mirror.consts.ContractMirrorConst.EMPTY_ADDRESS;

@Slf4j
public class BaseStableSwapPool extends AbstractCurve {
    private static final BigInteger FEE_DENOMINATOR = BigInteger.TEN.pow(10);
    private static final BigInteger PRECISION = BigInteger.TEN.pow(18);
    private static final BigInteger A_PRECISION = new BigInteger("100");
    private static final long BASE_CACHE_EXPIRES = 600;
    private int coinsCount;
    private int baseCoinsCount;
    @Getter
    private BigInteger[] rates;
    @Getter
    private BigInteger[] precisionMul;

    @Setter
    private StableSwapPoolData stableSwapPoolData;

    public static BaseStableSwapPool genInstance(ContractInfo contractInfo, IChainHelper iChainHelper,
                                                 IContractsHelper iContractsHelper, Map<String, String> sigMap) {
        ContractExtraData extraData = BaseStableSwapPool.parseToExtraData(contractInfo.getExtra());
        if (ObjectUtil.isNull(extraData)) {
            return null;
        }
        return new BaseStableSwapPool(contractInfo.getAddress(), contractInfo.getType(), extraData.getBaseCoinsCount(),
                extraData.getCoinsCount(), extraData.getRates(), extraData.getPrecisionMul(), iChainHelper, iContractsHelper, sigMap);
    }

    public BaseStableSwapPool(String address, ContractType type, int baseCoinsCount, int coinsCount, BigInteger[] rates,
                              BigInteger[] precisionMul, IChainHelper iChainHelper,
                              IContractsHelper iContractsHelper, Map<String, String> sigMap) {
        super(address, type, iChainHelper, iContractsHelper, sigMap);
        this.baseCoinsCount = baseCoinsCount;
        this.coinsCount = coinsCount;
        this.rates = rates;
        this.precisionMul = precisionMul;
    }


    private StableSwapPoolData getVarStableSwapBasePoolData() {
        if (ObjectUtil.isNull(stableSwapPoolData)) {
            stableSwapPoolData = new StableSwapPoolData(coinsCount, baseCoinsCount);
            stableSwapPoolData.setAddress(address);
            stableSwapPoolData.setType(type);
            stableSwapPoolData.setUsing(true);
            stableSwapPoolData.setReady(false);
            stableSwapPoolData.setAddExchangeContracts(false);
        }
        return stableSwapPoolData;
    }

    public StableSwapPoolData getCurveBasePoolData() {
        return getVarStableSwapBasePoolData().copySelf();
    }

    private void updateCoinsInfo(int count, String method, String[] coins, String[] names, String[] symbols) {
        for (int i = 0; i < count; i++) {
            String coinAddress = CallContractUtil.getTronAddressWithIndex(iChainHelper, ContractMirrorConst.EMPTY_ADDRESS,
                    address, method, BigInteger.valueOf(i));
            coins[i] = coinAddress;
            if (!coinAddress.equalsIgnoreCase(EMPTY_ADDRESS)) {
                String name = CallContractUtil.getString(iChainHelper, EMPTY_ADDRESS, coinAddress, "name");
                String symbol = CallContractUtil.getString(iChainHelper, EMPTY_ADDRESS, coinAddress, "symbol");
                names[i] = name;
                symbols[i] = symbol;
            }
        }
    }

    private void updateBalance(int count, String method, BigInteger[] balances) {
        for (int i = 0; i < count; i++) {
            BigInteger balance = CallContractUtil.getU256WithIndex(iChainHelper, ContractMirrorConst.EMPTY_ADDRESS,
                    address, method, BigInteger.valueOf(i));
            balances[i] = balance;
        }
    }

    @Override
    public String coins(int i) {
        return this.getVarStableSwapBasePoolData().getCoins()[i];
    }


    @Override
    public BigInteger fee() {
        return this.getVarStableSwapBasePoolData().getFee();
    }

    @Override
    public BigInteger getDyUnderlying(int i, int j, BigInteger dx, BigInteger dy) throws Exception {
        return null;
    }

    @Override
    public BigInteger addLiquidity(BigInteger[] amounts, BigInteger minMintAmount) throws Exception {
        return null;
    }

    @Override
    public BigInteger removeLiquidityOneCoin(BigInteger _token_amount, int i, BigInteger min_amount) throws Exception {
        return null;
    }

    @Override
    public BigInteger exchange(int i, int j, BigInteger dx, BigInteger min_dy) throws Exception {
        return null;
    }


    @Override
    public AbstractCurve copySelf() {
        StableSwapPoolData data = this.getCurveBasePoolData();
        BaseStableSwapPool pool = new BaseStableSwapPool(
                data.getAddress(),
                type,
                coinsCount,
                baseCoinsCount,
                copyBigInteger(rates),
                copyBigInteger(precisionMul),
                iChainHelper,
                iContractsHelper,
                sigMap
        );
        pool.setStableSwapPoolData(data);
        return pool;
    }

    @Override
    public boolean initDataFromChain1() {
        StableSwapPoolData data = this.getVarStableSwapBasePoolData();
        updateBalance(coinsCount, "balances", data.getBalances());
        updateCoinsInfo(coinsCount, "coins", data.getCoins(), data.getCoinNames(), data.getCoinSymbols());
        updateCoinsInfo(baseCoinsCount, "base_coins", data.getBaseCoins(), data.getBaseCoinNames(), data.getBaseCoinSymbols());
        BigInteger fee = CallContractUtil.getU256(iChainHelper, ContractMirrorConst.EMPTY_ADDRESS, address, "fee");
        BigInteger adminFee = CallContractUtil.getU256(iChainHelper, ContractMirrorConst.EMPTY_ADDRESS, address, "admin_fee");
        String owner = CallContractUtil.getTronAddress(iChainHelper, ContractMirrorConst.EMPTY_ADDRESS, address, "owner");
        String feeConverter = CallContractUtil.getTronAddress(iChainHelper, ContractMirrorConst.EMPTY_ADDRESS, address, "fee_converter");
        String lpToken = CallContractUtil.getTronAddress(iChainHelper, ContractMirrorConst.EMPTY_ADDRESS, address, "lp_token");
        String boolPool = CallContractUtil.getTronAddress(iChainHelper, ContractMirrorConst.EMPTY_ADDRESS, address, "base_pool");
        BigInteger baseVirtualPrice = CallContractUtil.getU256(iChainHelper, ContractMirrorConst.EMPTY_ADDRESS, address, "base_virtual_price");
        BigInteger baseCacheUpdated = CallContractUtil.getU256(iChainHelper, ContractMirrorConst.EMPTY_ADDRESS, address, "base_cache_updated");
        String baseLpToken = CallContractUtil.getTronAddress(iChainHelper, ContractMirrorConst.EMPTY_ADDRESS, address, "base_lp");
        BigInteger initialA = CallContractUtil.getU256(iChainHelper, ContractMirrorConst.EMPTY_ADDRESS, address, "initial_A");
        BigInteger futureA = CallContractUtil.getU256(iChainHelper, ContractMirrorConst.EMPTY_ADDRESS, address, "future_A");
        BigInteger initialATime = CallContractUtil.getU256(iChainHelper, ContractMirrorConst.EMPTY_ADDRESS, address, "initial_A_time");
        BigInteger futureATime = CallContractUtil.getU256(iChainHelper, ContractMirrorConst.EMPTY_ADDRESS, address, "future_A_time");
        BigInteger adminActionsDeadline = CallContractUtil.getU256(iChainHelper, ContractMirrorConst.EMPTY_ADDRESS, address, "admin_actions_deadline");
        BigInteger transferOwnershipDeadline = CallContractUtil.getU256(iChainHelper, ContractMirrorConst.EMPTY_ADDRESS, address, "transfer_ownership_deadline");
        BigInteger futureFee = CallContractUtil.getU256(iChainHelper, ContractMirrorConst.EMPTY_ADDRESS, address, "future_fee");
        BigInteger futureAdminFee = CallContractUtil.getU256(iChainHelper, ContractMirrorConst.EMPTY_ADDRESS, address, "future_admin_fee");
        String futureOwner = CallContractUtil.getTronAddress(iChainHelper, ContractMirrorConst.EMPTY_ADDRESS, address, "future_owner");
        BigInteger lpTotalSupply = CallContractUtil.getU256(iChainHelper, ContractMirrorConst.EMPTY_ADDRESS, lpToken, "totalSupply");
        BigInteger baseLpTotalSupply = CallContractUtil.getU256(iChainHelper, ContractMirrorConst.EMPTY_ADDRESS, baseLpToken, "totalSupply");
        data.setBaseLp(baseLpToken);
        data.setBasePool(boolPool);
        data.setLpToken(lpToken);
        data.setLpTotalSupply(lpTotalSupply);
        data.setBaseLpTotalSupply(baseLpTotalSupply);
        data.setFutureAdminFee(futureAdminFee);
        data.setBaseVirtualPrice(baseVirtualPrice);
        data.setBaseCacheUpdated(baseCacheUpdated);
        data.setFee(fee);
        data.setFutureFee(futureFee);
        data.setAdminFee(adminFee);
        data.setAdminActionsDeadline(adminActionsDeadline);
        data.setFeeConverter(feeConverter);
        data.setInitialA(initialA);
        data.setInitialATime(initialATime);
        data.setFutureA(futureA);
        data.setFutureATime(futureATime);
        data.setOwner(owner);
        data.setFutureOwner(futureOwner);
        data.setTransferOwnershipDeadline(transferOwnershipDeadline);
        isDirty = true;
        return true;
    }

    @Override
    public void updateBaseInfo(boolean isUsing, boolean isReady, boolean isAddExchangeContracts) {
        StableSwapPoolData data = this.getVarStableSwapBasePoolData();
        data.setUsing(isUsing);
        data.setReady(isReady);
        data.setAddExchangeContracts(isAddExchangeContracts);
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
        return (T) this.getVarStableSwapBasePoolData();
    }

    @Override
    public <T> T handleSpecialRequest(String method, String params) throws Exception {
        return null;
    }

    protected HandleResult handleEventTokenExchangeUnderlying(String[] topics, String data, HandleEventExtraData handleEventExtraData) {
        log.info("handleEventTokenExchangeUnderlying: {}, info: {} ", address, type, handleEventExtraData.getUniqueId(), this.getVarStableSwapBasePoolData());
        String body = Curve2PoolEvent.EVENT_NAME_TOKEN_EXCHANGE_UNDERLING_BODY;
        if (coinsCount == 3) {
            body = Curve3PoolEvent.EVENT_NAME_TOKEN_EXCHANGE_UNDERLING_BODY;
        }
        EventValues eventValues = getEventValue(EVENT_NAME_TOKEN_EXCHANGE_UNDERLING, body, topics, data, handleEventExtraData.getUniqueId());
        int i = ((BigInteger) eventValues.getNonIndexedValues().get(0).getValue()).intValue();
        BigInteger dx = (BigInteger) eventValues.getNonIndexedValues().get(1).getValue();
        int j = ((BigInteger) eventValues.getNonIndexedValues().get(2).getValue()).intValue();
        BigInteger dy = (BigInteger) eventValues.getNonIndexedValues().get(3).getValue();
        try {
            exchangeUnderlying(i, j, dx, dy, handleEventExtraData.getTimeStamp());
        } catch (Exception e) {
            return HandleResult.genHandleFailMessage(e.getMessage());
        }
        return HandleResult.genHandleSuccess();
    }

    protected HandleResult handleEventTokenExchange(String[] topics, String data, HandleEventExtraData handleEventExtraData) {
        log.info("handleEventTokenExchange: {}, info: {} ", address, type, handleEventExtraData.getUniqueId(), this.getVarStableSwapBasePoolData());
        StableSwapPoolData curveData = this.getVarStableSwapBasePoolData();
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
        rates[coinsCount - 1] = vpRate(handleEventExtraData.getTimeStamp());
        BigInteger dy = (BigInteger) eventValues.getNonIndexedValues().get(3).getValue();
        BigInteger tmp = dy.multiply(rates[j]).multiply(FEE_DENOMINATOR);
        BigInteger dyOri = (tmp.divide(FEE_DENOMINATOR.subtract(curveData.getFee()))).divide(PRECISION);
        BigInteger dyFee = dyOri.multiply(curveData.getFee()).divide(FEE_DENOMINATOR);
        BigInteger dyAdminFee = dyFee.multiply(curveData.getAdminFee()).divide(FEE_DENOMINATOR);
        dyAdminFee = dyAdminFee.multiply(PRECISION).divide(rates[j]);
        BigInteger dxWFee = getAmountWFee(i, dx);
        BigInteger newIBalance = curveData.getBalances()[i].add(dxWFee);
        BigInteger newJBalance = curveData.getBalances()[j].subtract(dy).subtract(dyAdminFee);
        curveData.updateBalances(i, newIBalance);
        curveData.updateBalances(j, newJBalance);
        this.isDirty = true;
        return HandleResult.genHandleSuccess();
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
        StableSwapPoolData curveData = this.getVarStableSwapBasePoolData();
        StaticArray<Uint256> amounts = (StaticArray<Uint256>) eventValues.getNonIndexedValues().get(0);
        StaticArray<Uint256> fees = (StaticArray<Uint256>) eventValues.getNonIndexedValues().get(1);

        List<Uint256> amountsNew = new ArrayList<>();
        for (int i = 0; i < coinsCount; i++) {
            BigInteger amountWFee = getAmountWFee(i, (BigInteger) amounts.getValue().get(i).getValue());
            BigInteger originBalance = curveData.getBalances()[i];
            BigInteger newBalance = originBalance.add(amountWFee);
            if (curveData.getLpTotalSupply().compareTo(BigInteger.ZERO) > 0) {
                BigInteger fee = fees.getValue().get(i).getValue();
                BigInteger newFee = fee.multiply(curveData.getAdminFee()).divide(FEE_DENOMINATOR);
                newBalance = newBalance.subtract(newFee);
            }
            curveData.updateBalances(i, newBalance);
            amountsNew.add(new Uint256(newBalance));
        }
        BigInteger newTotalSupply = (BigInteger) eventValues.getNonIndexedValues().get(3).getValue();
        curveData.setLpTotalSupply(newTotalSupply);
        this.isDirty = true;
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
        StableSwapPoolData curveData = this.getVarStableSwapBasePoolData();
        StaticArray<Uint256> amounts = (StaticArray<Uint256>) eventValues.getNonIndexedValues().get(0);
        List<Uint256> amountsNew = new ArrayList<>();
        for (int i = 0; i < coinsCount; i++) {
            BigInteger origin = curveData.getBalances()[i];
            BigInteger newBalance = origin.subtract((BigInteger) amounts.getValue().get(i).getValue());
            curveData.updateBalances(i, newBalance);
            amountsNew.add(new Uint256(newBalance));
        }
        BigInteger newTotalSupply = (BigInteger) eventValues.getNonIndexedValues().get(2).getValue();
        curveData.setLpTotalSupply(newTotalSupply);
        this.isDirty = true;
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
        StableSwapPoolData curveData = this.getVarStableSwapBasePoolData();
        StaticArray<Uint256> amounts = (StaticArray<Uint256>) eventValues.getNonIndexedValues().get(0);
        StaticArray<Uint256> fees = (StaticArray<Uint256>) eventValues.getNonIndexedValues().get(1);
        List<Uint256> amountsNew = new ArrayList<>();
        for (int i = 0; i < coinsCount; i++) {
            BigInteger originBalance = curveData.getBalances()[i];
            BigInteger fee = fees.getValue().get(i).getValue();
            BigInteger newBalance = originBalance.subtract(amounts.getValue().get(i).getValue());
            BigInteger newFee = fee.multiply(curveData.getAdminFee()).divide(FEE_DENOMINATOR);
            newBalance = newBalance.subtract(newFee);
            curveData.updateBalances(i, newBalance);
            amountsNew.add(new Uint256(newBalance));
        }
        BigInteger newTotalSupply = (BigInteger) eventValues.getNonIndexedValues().get(3).getValue();
        curveData.setLpTotalSupply(newTotalSupply);
        this.isDirty = true;
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
        StableSwapPoolData curveData = this.getVarStableSwapBasePoolData();
        BigInteger deadline = (BigInteger) eventValues.getIndexedValues().get(0).getValue();
        String admin = WalletUtil.hexStringToTron((String) eventValues.getIndexedValues().get(1).getValue());
        curveData.setOwner(admin);
        curveData.setTransferOwnershipDeadline(deadline);
        isDirty = true;
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
        StableSwapPoolData curveData = this.getVarStableSwapBasePoolData();
        curveData.setOwner(admin);
        curveData.setTransferOwnershipDeadline(BigInteger.ZERO);
        isDirty = true;
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
        StableSwapPoolData curveData = this.getVarStableSwapBasePoolData();
        curveData.setFeeConverter(feeConverter);
        isDirty = true;
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
        StableSwapPoolData curveData = this.getVarStableSwapBasePoolData();
        BigInteger deadLine = (BigInteger) eventValues.getIndexedValues().get(0).getValue();
        BigInteger fee = (BigInteger) eventValues.getNonIndexedValues().get(0).getValue();
        BigInteger adminFee = (BigInteger) eventValues.getNonIndexedValues().get(1).getValue();
        curveData.setFee(fee);
        curveData.setAdminFee(adminFee);
        curveData.setAdminActionsDeadline(deadLine);
        isDirty = true;
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
        StableSwapPoolData curveData = this.getVarStableSwapBasePoolData();
        BigInteger fee = (BigInteger) eventValues.getNonIndexedValues().get(0).getValue();
        BigInteger adminFee = (BigInteger) eventValues.getNonIndexedValues().get(1).getValue();
        curveData.setFee(fee);
        curveData.setAdminFee(adminFee);
        curveData.setAdminActionsDeadline(BigInteger.ZERO);
        isDirty = true;
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
        StableSwapPoolData curveData = this.getVarStableSwapBasePoolData();
        BigInteger a = (BigInteger) eventValues.getNonIndexedValues().get(0).getValue();
        BigInteger af = (BigInteger) eventValues.getNonIndexedValues().get(1).getValue();
        BigInteger aT = (BigInteger) eventValues.getNonIndexedValues().get(2).getValue();
        BigInteger afT = (BigInteger) eventValues.getNonIndexedValues().get(3).getValue();
        curveData.setInitialA(a);
        curveData.setInitialATime(aT);
        curveData.setFutureATime(afT);
        curveData.setFutureA(af);
        isDirty = true;
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
        StableSwapPoolData curveData = this.getVarStableSwapBasePoolData();
        BigInteger a = (BigInteger) eventValues.getNonIndexedValues().get(0).getValue();
        BigInteger aTime = (BigInteger) eventValues.getNonIndexedValues().get(1).getValue();
        curveData.setFutureA(a);
        curveData.setInitialA(BigInteger.valueOf(a.longValue()));
        curveData.setInitialATime(aTime);
        curveData.setInitialATime(BigInteger.valueOf(aTime.longValue()));
        isDirty = true;
        return HandleResult.genHandleSuccess();
    }

    public BigInteger exchangeUnderlying(int i, int j, BigInteger _dx, BigInteger mindy, long timestamp) throws Exception {
        StableSwapPoolData data = this.getVarStableSwapBasePoolData();
        BigInteger[] rates = this.getRates();
        String basePool = data.getBasePool();
        int maxCoin = coinsCount - 1;
        rates[maxCoin] = vpRate(timestamp);
        int baseI = i - maxCoin;
        int baseJ = j - maxCoin;
        int metaI = maxCoin;
        int metaJ = maxCoin;
        if (baseI < 0) {
            metaI = i;
        }
        if (baseJ < 0) {
            metaJ = j;
        }
        BigInteger dy = BigInteger.ZERO;
        String inputCoin, outputCoin;
        if (baseI < 0) {
            inputCoin = data.getCoins()[i];
        } else {
            inputCoin = data.getBaseCoins()[baseI];
        }

        if (baseJ < 0) {
            outputCoin = data.getCoins()[j];
        } else {
            outputCoin = data.getBaseCoins()[baseJ];
        }
        BigInteger dxWFee = getAmountWFee(i, _dx);

        if (baseI < 0 || baseJ < 0) {
            BigInteger[] xp = xpMem(rates[maxCoin], data.getBalances());
            BigInteger x = BigInteger.ZERO;
            if (baseI < 0) {
                x = xp[i].add(dxWFee.multiply(rates[i]).divide(PRECISION));
            } else {
                BigInteger[] baseIputs = empty(baseCoinsCount);
                baseIputs[baseI] = dxWFee;
                AbstractCurve curve = ((AbstractCurve) iContractsHelper.getContract(basePool)).copySelf();
                dxWFee = curve.addLiquidity(baseIputs, BigInteger.ZERO);
                x = dxWFee.multiply(rates[maxCoin]).divide(PRECISION);
                x = x.add(xp[maxCoin]);
            }
            BigInteger y = getY(metaI, metaJ, x, xp, timestamp);
            dy = xp[metaJ].subtract(y).subtract(BigInteger.ONE);
            BigInteger dyFee = dy.multiply(data.getFee()).divide(FEE_DENOMINATOR);
            dy = dy.subtract(dyFee).multiply(PRECISION).divide(rates[metaJ]);
            BigInteger dyAdminFee = dyFee.multiply(data.getAdminFee()).divide(FEE_DENOMINATOR);
            dyAdminFee = dyAdminFee.multiply(PRECISION).divide(rates[metaJ]);
            BigInteger balanceI = data.getBalances()[metaI].add(dxWFee);
            BigInteger balanceJ = data.getBalances()[metaJ].subtract(dy).subtract(dyAdminFee);
            System.out.println(balanceI);
            System.out.println(balanceJ);
            data.updateBalances(metaI, balanceI);
            data.updateBalances(metaJ, balanceJ);

            if (baseJ >= 0) {
                //dy= 31721347864730507
                AbstractCurve curve = ((AbstractCurve) iContractsHelper.getContract(basePool)).copySelf();
                dy = curve.removeLiquidityOneCoin(dy, baseJ, BigInteger.ZERO);
            }

            if (dy.compareTo(mindy) < 0) {
                throw new Exception("Too few coins in result");
            }
        } else {
            AbstractCurve curve = ((AbstractCurve) iContractsHelper.getContract(basePool)).copySelf();
            dy = curve.exchange(baseI, baseJ, dxWFee, mindy);
        }
        return dy;
    }


    private BigInteger getAmountWFee(int _tokenId, BigInteger dx) {
        // 查看CurvePoolV2 feeIndex 是USDT 不是特殊收费ERC20，之后需要在添加
        return dx;
    }

    public BigInteger a(long timestamp) {
        StableSwapPoolData data = this.getVarStableSwapBasePoolData();
        BigInteger t1 = data.getFutureATime();
        BigInteger a1 = data.getFutureA();
        if (timestamp < t1.longValue()) {
            BigInteger a0 = data.getInitialA();
            BigInteger t0 = data.getInitialATime();
            BigInteger tCast = BigInteger.valueOf(timestamp).subtract(t0);
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

    private BigInteger[] xp(BigInteger vpRate) {
        StableSwapPoolData data = this.getVarStableSwapBasePoolData();
        BigInteger[] result = getRates();
        result[coinsCount - 1] = vpRate;
        BigInteger[] balances = data.getBalances();
        for (int i = 0; i < coinsCount; i++) {
            result[i] = result[i].multiply(balances[i]).divide(PRECISION);
        }
        return result;
    }

    private BigInteger[] xpMem(BigInteger vpRate, BigInteger[] balances) {
        BigInteger[] result = getRates();
        result[coinsCount - 1] = vpRate;
        for (int i = 0; i < coinsCount; i++) {
            result[i] = result[i].multiply(balances[i]).divide(PRECISION);
        }
        return result;
    }

    private BigInteger vpRate(long timeStamp) {
        StableSwapPoolData data = this.getVarStableSwapBasePoolData();
        try {
            if (timeStamp > data.getBaseCacheUpdated().longValue() + BASE_CACHE_EXPIRES) {
                AbstractCurve curve = ((AbstractCurve) this.iContractsHelper.getContract(data.getBasePool())).copySelf();
                BigInteger price = curve.getVirtualPrice(0);

                data.setBaseVirtualPrice(price);
                data.setBaseCacheUpdated(BigInteger.valueOf(timeStamp));
            }
        } catch (Exception e) {

        }
        return data.getBaseVirtualPrice();
    }

    private BigInteger vpRateRo(long timeStamp) {
        StableSwapPoolData data = this.getVarStableSwapBasePoolData();
        try {
            if (timeStamp > data.getBaseCacheUpdated().longValue() + BASE_CACHE_EXPIRES) {
                AbstractCurve curve = ((AbstractCurve) this.iContractsHelper.getContract(data.getBasePool())).copySelf();
                return curve.getVirtualPrice(0);
            }
        } catch (Exception e) {

        }
        return data.getBaseVirtualPrice();
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
            BigInteger tmp = ((ann.multiply(s).divide(A_PRECISION)).add(dp.multiply(BigInteger.valueOf(coinsCount)))).multiply(d);
            d = tmp.divide(ann.subtract(A_PRECISION).multiply(d).divide(A_PRECISION).add(BigInteger.valueOf(coinsCount + 1).multiply(dp)));
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

    protected BigInteger getDMem(BigInteger vpRate, BigInteger[] balances, BigInteger amp) throws Exception {
        return getD(xpMem(vpRate, balances), amp);
    }

    public BigInteger getVirtualPrice(long timestamp) throws Exception {
        StableSwapPoolData data = this.getVarStableSwapBasePoolData();
        BigInteger amp = a(timestamp);
        BigInteger vpRate = vpRateRo(timestamp);
        BigInteger[] xp = xp(vpRate);
        BigInteger d = getD(xp, amp);
        BigInteger totalSupply = data.getLpTotalSupply();
        return d.multiply(PRECISION).divide(totalSupply);
    }

    public BigInteger calcTokenAmount(long timestamp, BigInteger[] amounts, boolean deposit) throws Exception {
        StableSwapPoolData data = this.getVarStableSwapBasePoolData();
        BigInteger[] balances = data.getCopyBalances();
        BigInteger amp = a(timestamp);
        BigInteger vpRate = vpRateRo(timestamp);
        BigInteger d0 = getDMem(vpRate, balances, amp);
        for (int i = 0; i < coinsCount; i++) {
            if (deposit) {
                balances[i] = balances[i].add(amounts[i]);
            } else {
                balances[i] = balances[i].subtract(amounts[i]);
            }
        }
        BigInteger d1 = getDMem(vpRate, balances, amp);
        BigInteger tokenAmount = data.getLpTotalSupply();
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
        c = c.multiply(d).multiply(A_PRECISION).divide(ann.multiply(nCoins));
        BigInteger b = s_.add(d.multiply(A_PRECISION).divide(ann));
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
        StableSwapPoolData data = this.getVarStableSwapBasePoolData();
        BigInteger[] rates = getRates();
        rates[coinsCount - 1] = vpRateRo(timestamp);
        BigInteger[] xp = xp(rates[coinsCount - 1]);
        BigInteger x = xp[i].add(dx.multiply(rates[i]).divide(PRECISION));
        BigInteger y = getY(i, j, x, xp, timestamp);
        BigInteger dy = xp[j].subtract(y).subtract(BigInteger.ONE);
        BigInteger fee = data.getFee().multiply(dy).divide(FEE_DENOMINATOR);
        return dy.subtract(fee).multiply(PRECISION).divide(FEE_DENOMINATOR);
    }

    private BigInteger[] empty(int num) {
        BigInteger[] res = new BigInteger[num];
        for (int i = 0; i < num; i++) {
            res[i] = BigInteger.ZERO;
        }
        return res;
    }

    private BigInteger[] copyBigInteger(BigInteger[] src) {
        BigInteger[] res = new BigInteger[src.length];
        for (int i = 0; i < src.length; i++) {
            res[i] = new BigInteger(src[i].toString());
        }
        return res;
    }

    public BigInteger getDyUnderLying(int i, int j, BigInteger _dx, long timestamp) throws Exception {
        StableSwapPoolData data = this.getVarStableSwapBasePoolData();
        int maxCoin = coinsCount - 1;
        BigInteger vpRate = vpRateRo(timestamp);
        BigInteger[] xp = xp(vpRate);
        BigInteger[] precisions = getPrecisionMul();
        int baseI = i - maxCoin;
        int baseJ = j - maxCoin;
        int metaI = maxCoin;
        int metaJ = maxCoin;
        if (baseI < 0) {
            metaI = i;
        }
        if (baseJ < 0) {
            metaJ = j;
        }
        BigInteger x = BigInteger.ZERO;
        if (baseI < 0) {
            x = xp[i].add(_dx.multiply(precisions[i]));
        } else {
            if (baseJ < 0) {
                BigInteger[] baseInput = empty(baseCoinsCount);
                baseInput[baseI] = _dx;
                AbstractCurve curve = ((AbstractCurve) iContractsHelper.getContract(data.getBasePool())).copySelf();
                x = (curve).calcTokenAmount(0, baseInput, true).multiply(vpRate).divide(PRECISION);
                x = x.subtract(x.multiply(curve.fee()).divide(BigInteger.TWO.multiply(FEE_DENOMINATOR)));
                x = x.add(xp[maxCoin]);
            } else {
                return getDy(baseI, baseJ, _dx, timestamp);
            }
        }

        BigInteger y = getY(metaI, metaJ, x, xp, timestamp);
        BigInteger dy = xp[metaJ].subtract(y).subtract(BigInteger.ONE);
        dy = dy.subtract(data.getFee().multiply(dy).divide(FEE_DENOMINATOR));
        if (baseJ < 0) {
            dy = dy.divide(precisions[metaJ]);
        } else {
            dy = calcWithdrawOneCoin(timestamp, dy.multiply(PRECISION).divide(vpRate), baseJ);
        }

        return dy;
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
        c = c.multiply(d).multiply(A_PRECISION).divide(ann.multiply(nCoins));
        BigInteger b = s_.add(d.multiply(A_PRECISION).divide(ann));
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

    private BigInteger[] localCalcWithdrawOneCoin(BigInteger _token_amount, int i, BigInteger vpRate, long timestamp, StableSwapPoolData data) throws Exception {
        BigInteger nCoins = BigInteger.valueOf(coinsCount);
        BigInteger amp = a(timestamp);
        BigInteger[] xp = xp(vpRate);
        BigInteger d0 = getD(xp, amp);
        BigInteger totalSupply = data.getLpTotalSupply();
        BigInteger d1 = d0.subtract(_token_amount.multiply(d0).divide(totalSupply));
        BigInteger newY = getYD(amp, i, xp, d1);
        BigInteger _fee = data.getFee().multiply(nCoins).divide(BigInteger.valueOf((coinsCount - 1) * 4));

        BigInteger[] rates = getRates();
        rates[coinsCount - 1] = vpRate;
        BigInteger[] xpReduced = xp(vpRate);
        BigInteger dy0 = (xp[i].subtract(newY)).multiply(PRECISION).divide(rates[i]);  // w/o fees

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
        dy = (dy.subtract(BigInteger.ONE)).multiply(PRECISION).divide(rates[i]);
        return new BigInteger[]{dy, dy0.subtract(dy), totalSupply};
    }

    public BigInteger calcWithdrawOneCoin(long timestamp, BigInteger _token_amount, int i) throws Exception {
        BigInteger vpRate = vpRateRo(timestamp);
        BigInteger[] res = localCalcWithdrawOneCoin(_token_amount, i, vpRate, timestamp, this.getVarStableSwapBasePoolData().copySelf());
        return res[0];
    }

    @Data
    public static class ContractExtraData {
        private int coinsCount;
        private int baseCoinsCount;
        private BigInteger[] rates;
        private BigInteger[] precisionMul;
    }

    public static ContractExtraData parseToExtraData(String input) {
        try {
            ContractExtraData res = GsonUtil.gsonToObject(input, ContractExtraData.class);
            return res;
        } catch (Exception e) {
            log.error("Parse base stable swap pool failed, input:{} err:{}", e);
        }
        return null;
    }


}
