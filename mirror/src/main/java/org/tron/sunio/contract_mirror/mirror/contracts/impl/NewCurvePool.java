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
import org.tron.sunio.contract_mirror.mirror.contracts.BaseContract;
import org.tron.sunio.contract_mirror.mirror.contracts.ContractInfo;
import org.tron.sunio.contract_mirror.mirror.contracts.IContractsHelper;
import org.tron.sunio.contract_mirror.mirror.dao.NewCurvePoolData;
import org.tron.sunio.contract_mirror.mirror.enums.ContractType;
import org.tron.sunio.contract_mirror.mirror.tools.CallContractUtil;
import org.tron.sunio.tronsdk.WalletUtil;
import org.web3j.abi.EventValues;
import org.web3j.abi.FunctionEncoder;
import org.web3j.abi.datatypes.StaticArray;
import org.web3j.abi.datatypes.generated.StaticArray2;
import org.web3j.abi.datatypes.generated.StaticArray3;
import org.web3j.abi.datatypes.generated.Uint256;
import org.web3j.utils.Strings;

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
public class NewCurvePool extends AbstractCurve {
    private static final BigInteger FEE_DENOMINATOR = BigInteger.TEN.pow(10);
    private static final BigInteger PRECISION = BigInteger.TEN.pow(18);
    private static final BigInteger A_PRECISION = new BigInteger("100");
    private static final long BASE_CACHE_EXPIRES = 600;
    @Getter
    private int coinsCount;
    @Getter
    private int baseCoinsCount;
    @Getter
    private BigInteger[] rates;
    @Getter
    private BigInteger[] precisionMul;
    @Setter
    private NewCurvePoolData newCurvePoolData;
    @Setter
    private NewCurvePoolData preSwapPoolData;

    public static NewCurvePool genInstance(ContractInfo contractInfo, IChainHelper iChainHelper,
                                           IContractsHelper iContractsHelper, Map<String, String> sigMap) {
        ContractExtraData extraData = NewCurvePool.parseToExtraData(contractInfo.getExtra());
        if (ObjectUtil.isNull(extraData)) {
            return null;
        }
        return new NewCurvePool(contractInfo.getAddress(), contractInfo.getType(), extraData.getBaseCoinsCount(),
                extraData.getCoinsCount(), extraData.getRates(), extraData.getPrecisionMul(), extraData.getPoolName(), iChainHelper, iContractsHelper, sigMap);
    }

    public NewCurvePool(String address, ContractType type, int baseCoinsCount, int coinsCount, BigInteger[] rates,
                        BigInteger[] precisionMul, String poolName, IChainHelper iChainHelper,
                        IContractsHelper iContractsHelper, Map<String, String> sigMap) {
        super(address, type, iChainHelper, iContractsHelper, sigMap);
        this.baseCoinsCount = baseCoinsCount;
        this.coinsCount = coinsCount;
        this.rates = rates;
        this.precisionMul = precisionMul;
        this.poolName = poolName;
    }

    public NewCurvePoolData getVarNewCurvePoolData(String uniqueId) {
        NewCurvePoolData data = getVarNewCurvePoolData();
        if (Strings.isEmpty(uniqueId) || ObjectUtil.isNull(preSwapPoolData)) {
            return data;
        }
        String[] idInfo = uniqueId.split("_");
        if (idInfo.length < 2) {
            return data;
        }
        if (idInfo[0].equals(currentTx) && !idInfo[1].equals(currentIndex)) {
            return preSwapPoolData;
        }
        return data;
    }

    public NewCurvePoolData getVarNewCurvePoolData() {
        if (ObjectUtil.isNull(newCurvePoolData)) {
            newCurvePoolData = new NewCurvePoolData(coinsCount, baseCoinsCount);
            newCurvePoolData.setAddress(address);
            newCurvePoolData.setPoolName(poolName);
            newCurvePoolData.setType(type);
            newCurvePoolData.setUsing(true);
            newCurvePoolData.setReady(false);
            newCurvePoolData.setAddExchangeContracts(false);
        }
        return newCurvePoolData;
    }

    public NewCurvePoolData getCurveBasePoolData() {
        return getVarNewCurvePoolData().copySelf();
    }

    private void updateCoinsInfo(int count, String method, String[] coins, String[] names, String[] symbols, long[] decimals) {
        for (int i = 0; i < count; i++) {
            String coinAddress = CallContractUtil.getTronAddressWithIndex(iChainHelper, ContractMirrorConst.EMPTY_ADDRESS,
                    address, method, BigInteger.valueOf(i));
            coins[i] = coinAddress;
            if (!coinAddress.equalsIgnoreCase(EMPTY_ADDRESS)) {
                String name = CallContractUtil.getString(iChainHelper, EMPTY_ADDRESS, coinAddress, "name");
                String symbol = CallContractUtil.getString(iChainHelper, EMPTY_ADDRESS, coinAddress, "symbol");
                long decimal = CallContractUtil.getU256(iChainHelper, EMPTY_ADDRESS, coinAddress, "decimals").longValue();
                names[i] = name;
                symbols[i] = symbol;
                decimals[i] = decimal;
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
        return this.getVarNewCurvePoolData().getCoins()[i];
    }


    @Override
    public BigInteger fee() {
        return this.getVarNewCurvePoolData().getFee();
    }

    @Override
    public BigInteger adminFee() {
        return this.getVarNewCurvePoolData().getAdminFee();
    }

    @Override
    public BigInteger[] rates(String uniqueId, long timestamp, IContractsHelper iContractsHelper) {
        BigInteger[] rates = copyBigIntegerArray(this.rates);
        rates[coinsCount - 1] = vpRate(uniqueId, timestamp, iContractsHelper);
        return rates;
    }


    @Override
    public BigInteger addLiquidity(String uniqueId, BigInteger[] amounts, BigInteger minMintAmount, long timestamp, IContractsHelper iContractsHelper) throws Exception {

        NewCurvePoolData poolData = this.getVarNewCurvePoolData(uniqueId);
        BigInteger amp = a(uniqueId, timestamp);
        BigInteger vpRate = vpRate(uniqueId, timestamp, iContractsHelper);
        BigInteger[] oldBalances = copyBigIntegerArray(poolData.getBalances());

        // Initial invariant
        BigInteger tokenSupply = poolData.getLpTotalSupply();
        BigInteger d0 = BigInteger.ZERO;
        if (tokenSupply.compareTo(BigInteger.ZERO) > 0) {
            d0 = getDMem(vpRate, oldBalances, amp);
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

        BigInteger d1 = getDMem(vpRate, newBalances, amp);
        if (d1.compareTo(d0) <= 0) {
            throw new Exception("D1 must gt D0");
        }
        BigInteger mintAmount = BigInteger.ZERO;
        BigInteger[] fees = empty(coinsCount);
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

            d2 = getDMem(vpRate, newBalances, amp);
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
        poolData.setLpTotalSupply(poolData.getLpTotalSupply().add(mintAmount));
        return mintAmount;
    }

    @Override
    public BigInteger removeLiquidityOneCoin(String uniqueId, BigInteger _token_amount, int i, BigInteger min_amount, long timestamp, IContractsHelper iContractsHelper) throws Exception {
        NewCurvePoolData poolData = this.getVarNewCurvePoolData(uniqueId);
        BigInteger vpRate = vpRate(uniqueId, timestamp, iContractsHelper);
        BigInteger[] calRes = localCalcWithdrawOneCoin(uniqueId, _token_amount, i, vpRate, timestamp, poolData);
        if (calRes[0].compareTo(min_amount) < 0) {
            new Exception("Not enough coins removed");
        }
        poolData.getBalances()[i] = poolData.getBalances()[i].subtract(calRes[0].add(calRes[1].multiply(
                poolData.getAdminFee()).divide(FEE_DENOMINATOR)));
        poolData.setLpTotalSupply(poolData.getLpTotalSupply().subtract(_token_amount));
        return calRes[0];
    }

    @Override
    public BigInteger exchange(String uniqueId, int i, int j, BigInteger dx, BigInteger min_dy, long timestamp, IContractsHelper iContractsHelper) throws Exception {
        NewCurvePoolData poolData = this.getVarNewCurvePoolData(uniqueId);
        int maxCoin = coinsCount - 1;
        BigInteger[] rates = getRates();
        rates[maxCoin] = vpRate(uniqueId, timestamp, iContractsHelper);
        BigInteger[] oldBalances = copyBigIntegerArray(poolData.getBalances());
        BigInteger[] xp = xpMem(rates[maxCoin], oldBalances);
        BigInteger dxWFee = getAmountWFee(i, dx);
        BigInteger x = xp[i].add(dxWFee.multiply(rates[i]).divide(PRECISION));
        BigInteger y = getY(uniqueId, i, j, x, xp, timestamp);

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

    @Override
    public BaseContract copySelf() {
        try {
            rlock.lock();
            NewCurvePoolData data = this.getCurveBasePoolData();
            NewCurvePool pool = new NewCurvePool(
                    data.getAddress(),
                    type,
                    baseCoinsCount,
                    coinsCount,
                    copyBigIntegerArray(rates),
                    copyBigIntegerArray(precisionMul),
                    poolName,
                    iChainHelper,
                    iContractsHelper,
                    sigMap
            );
            pool.setNewCurvePoolData(data);
            pool.setReady(this.isReady);
            pool.setAddExchangeContracts(this.isAddExchangeContracts);
            pool.setDirty(this.isDirty);
            pool.setUsing(this.isUsing);
            if (ObjectUtil.isNotNull(preSwapPoolData)) {
                pool.setPreSwapPoolData(preSwapPoolData.copySelf());
                pool.setCurrentTx(currentTx);
                pool.setCurrentIndex(currentIndex);
            }
            return pool;
        } finally {
            rlock.unlock();
        }
    }

    /**
     * uint256 dy_fee = dy.mul(fee).div(FEE_DENOMINATOR);
     * dy = (dy.sub(dy_fee)).mul(PRECISION).div(rates[j]);
     * require(dy >= min_dy, "Exchange resulted in fewer coins than expected");
     * <p>
     * dy_admin_fee = dy_fee.mul(admin_fee).div(FEE_DENOMINATOR);
     * dy_admin_fee = dy_admin_fee.mul(PRECISION).div(rates[j]);
     */
    @Override
    public double calcFee(String uniqueId, long timestamp, int j, IContractsHelper iContractsHelper) {
//        BigInteger[] rates = this.rates(uniqueId, timestamp, iContractsHelper);
//        BigDecimal feeV = new BigDecimal(this.getVarStableSwapBasePoolData(uniqueId).getFee());
//        BigDecimal adminFeeV = new BigDecimal(this.getVarStableSwapBasePoolData(uniqueId).getAdminFee());
//        BigDecimal fee = feeV.divide(new BigDecimal(FEE_DENOMINATOR), 6, RoundingMode.UP);
//        BigDecimal feeAdmin = fee.multiply(adminFeeV).multiply(new BigDecimal(PRECISION))
//                .divide(new BigDecimal(FEE_DENOMINATOR), 6, RoundingMode.UP).divide(new BigDecimal(rates[j]), 6, RoundingMode.UP);
//        return fee.add(feeAdmin).doubleValue();
        return 0.0004;
    }

    @Override
    public double calcBasePoolFee(String uniqueId, long timestamp, int j, IContractsHelper iContractsHelper) {
//        AbstractCurve curve = (AbstractCurve) iContractsHelper.getContract(this.getVarStableSwapBasePoolData(uniqueId).getBasePool());
//        return curve.calcFee(uniqueId, timestamp, j, iContractsHelper);
        return 0.0004;
    }

    @Override
    public boolean initDataFromChain1() {
        NewCurvePoolData data = this.getVarNewCurvePoolData();
        updateBalance(coinsCount, "balances", data.getBalances());
        updateCoinsInfo(coinsCount, "coins", data.getCoins(), data.getCoinNames(), data.getCoinSymbols(), data.getCoinDecimals());
        updateCoinsInfo(baseCoinsCount, "base_coins", data.getBaseCoins(), data.getBaseCoinNames(), data.getBaseCoinSymbols(), data.getBaseCoinDecimals());
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
        NewCurvePoolData data = this.getVarNewCurvePoolData();
        data.setUsing(isUsing);
        data.setReady(isReady);
        data.setAddExchangeContracts(isAddExchangeContracts);
        isDirty = true;
    }

    @Override
    protected void saveUpdateToCache() {

    }

    private void updatePreDataInfo(String uniqueId) {
        if (Strings.isEmpty(uniqueId)) {
            return;
        }
        String[] infos = uniqueId.split("_");
        if (infos.length < 2) {
            return;
        }
        currentTx = infos[0];
        currentIndex = infos[1];
        preSwapPoolData = this.getVarNewCurvePoolData().copySelf();
    }

    @Override
    protected HandleResult handleEvent1(String eventName, String[] topics, String data, HandleEventExtraData handleEventExtraData) {
        HandleResult result;
        if (!iContractsHelper.isContractReady(this.getVarNewCurvePoolData(handleEventExtraData.getUniqueId()).getBasePool())) {
            this.isDirty = true;
            updateBaseInfo(isUsing, false, isAddExchangeContracts);
            this.isReady = false;
            this.isDirty = true;
            return HandleResult.genHandleFailMessage(String.format("Event:%s not handle,as base contract not ready", handleEventExtraData.getUniqueId()));
        }
        updatePreDataInfo(handleEventExtraData.getUniqueId());
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
    @SuppressWarnings("unchecked")
    public <T> T getStatus() {
        return (T) this.getVarNewCurvePoolData();
    }

    @Override
    public String getVersion() {
        return this.poolName;
    }

    @Override
    public <T> T handleSpecialRequest(String method, String params) throws Exception {
        return null;
    }

    protected HandleResult handleEventTokenExchangeUnderlying(String[] topics, String data, HandleEventExtraData handleEventExtraData) {
        log.info("handleEventTokenExchangeUnderlying: {}, type: {} id: {} , topic:{}, data:{}", address, type, handleEventExtraData.getUniqueId(), topics, data);
        String body = Curve2PoolEvent.EVENT_NAME_TOKEN_EXCHANGE_UNDERLING_BODY;
        if (coinsCount == 3) {
            body = Curve3PoolEvent.EVENT_NAME_TOKEN_EXCHANGE_UNDERLING_BODY;
        }
        EventValues eventValues = getEventValue(EVENT_NAME_TOKEN_EXCHANGE_UNDERLING, body, topics, data, handleEventExtraData.getUniqueId());
        int i = ((BigInteger) eventValues.getNonIndexedValues().get(0).getValue()).intValue();
        BigInteger dx = (BigInteger) eventValues.getNonIndexedValues().get(1).getValue();
        int j = ((BigInteger) eventValues.getNonIndexedValues().get(2).getValue()).intValue();
        BigInteger dy = (BigInteger) eventValues.getNonIndexedValues().get(3).getValue();
        log.info("handleEventTokenExchangeUnderlying: {}, {}, {}", i, j, dy);
        try {
            exchangeUnderlying(handleEventExtraData.getUniqueId(), i, j, dx, dy, handleEventExtraData.getTimeStamp(), this.iContractsHelper);
        } catch (Exception e) {
            return HandleResult.genHandleFailMessage(e.getMessage());
        }
        return HandleResult.genHandleSuccess();
    }

    protected HandleResult handleEventTokenExchange(String[] topics, String data, HandleEventExtraData handleEventExtraData) {
        String uniqueId = handleEventExtraData.getUniqueId();
        log.info("handleEventTokenExchange: {}, info: {} , {}, {}", address, type, uniqueId, this.getVarNewCurvePoolData(uniqueId));
        NewCurvePoolData curveData = this.getVarNewCurvePoolData(uniqueId);
        String body = Curve2PoolEvent.EVENT_NAME_TOKEN_EXCHANGE_BODY;
        if (coinsCount == 3) {
            body = Curve3PoolEvent.EVENT_NAME_TOKEN_EXCHANGE_BODY;
        }
        EventValues eventValues = getEventValue(EVENT_NAME_TOKEN_EXCHANGE, body, topics, data, uniqueId);
        if (ObjectUtil.isNull(eventValues)) {
            return HandleResult.genHandleFailMessage(String.format("Contract%s, type:%s decode handleEventTokenExchange fail!, unique id :%s",
                    address, type, handleEventExtraData.getUniqueId()));
        }
        try {
            vpRate(uniqueId, handleEventExtraData.getTimeStamp(), this.iContractsHelper);
            int i = ((BigInteger) eventValues.getNonIndexedValues().get(0).getValue()).intValue();
            BigInteger dx = (BigInteger) eventValues.getNonIndexedValues().get(1).getValue();
            int j = ((BigInteger) eventValues.getNonIndexedValues().get(2).getValue()).intValue();
            BigInteger[] rates = copyBigIntegerArray(this.rates);
            rates[coinsCount - 1] = vpRate(uniqueId, handleEventExtraData.getTimeStamp(), this.iContractsHelper);
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
        } catch (Exception e) {
            return HandleResult.genHandleFailMessage(e.getMessage());
        }
    }

    protected HandleResult handleEventAddLiquidity(String[] topics, String data, HandleEventExtraData handleEventExtraData) {
        log.info("{}:{} handleEventAddLiquidity:{}, {}, {}", address, type, handleEventExtraData.getUniqueId());
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
        try {
            NewCurvePoolData curveData = this.getVarNewCurvePoolData(handleEventExtraData.getUniqueId());
            StaticArray<Uint256> amounts = (StaticArray<Uint256>) eventValues.getNonIndexedValues().get(0);
            StaticArray<Uint256> fees = (StaticArray<Uint256>) eventValues.getNonIndexedValues().get(1);
            vpRate(handleEventExtraData.getUniqueId(), handleEventExtraData.getTimeStamp(), this.iContractsHelper);
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
        } catch (Exception e) {
            return HandleResult.genHandleFailMessage(e.getMessage());
        }

    }

    protected HandleResult handleEventRemoveLiquidity(String[] topics, String data, HandleEventExtraData handleEventExtraData) {
        log.info("{}:{} handleEventRemoveLiquidity:{}, {}, {}", address, type, handleEventExtraData.getUniqueId());
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
        try {
            NewCurvePoolData curveData = this.getVarNewCurvePoolData(handleEventExtraData.getUniqueId());
            StaticArray<Uint256> amounts = (StaticArray<Uint256>) eventValues.getNonIndexedValues().get(0);
            vpRate(handleEventExtraData.getUniqueId(), handleEventExtraData.getTimeStamp(), this.iContractsHelper);
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
        } catch (Exception e) {
            return HandleResult.genHandleFailMessage(e.getMessage());
        }
    }

    protected HandleResult handleEventRemoveLiquidityOne(String[] topics, String data, HandleEventExtraData handleEventExtraData) {
        log.info("{}:{} handleEventRemoveLiquidityOne:{}:{},{}", address, type, handleEventExtraData.getUniqueId());
        updateBaseInfo(isUsing, false, isAddExchangeContracts);
        this.isReady = false;
        this.isDirty = true;
        return HandleResult.genHandleSuccess();
    }

    protected HandleResult handleEventRemoveLiquidityImbalance(String[] topics, String data, HandleEventExtraData handleEventExtraData) {
        log.info("{}:{} handleEventRemoveLiquidityImbalance:{}, {}, {}", address, type, handleEventExtraData.getUniqueId());
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
        try {
            NewCurvePoolData curveData = this.getVarNewCurvePoolData(handleEventExtraData.getUniqueId());
            StaticArray<Uint256> amounts = (StaticArray<Uint256>) eventValues.getNonIndexedValues().get(0);
            StaticArray<Uint256> fees = (StaticArray<Uint256>) eventValues.getNonIndexedValues().get(1);
            vpRate(handleEventExtraData.getUniqueId(), handleEventExtraData.getTimeStamp(), this.iContractsHelper);
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
        } catch (Exception e) {
            return HandleResult.genHandleFailMessage(e.getMessage());
        }
    }

    protected HandleResult handleEventCommitNewAdmin(String[] topics, String data, HandleEventExtraData handleEventExtraData) {
        log.info("{}:{} handleEventCommitNewAdmin:{}, {}, {}", address, type, handleEventExtraData.getUniqueId());
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
        NewCurvePoolData curveData = this.getVarNewCurvePoolData(handleEventExtraData.getUniqueId());
        BigInteger deadline = (BigInteger) eventValues.getIndexedValues().get(0).getValue();
        String admin = WalletUtil.hexStringToTron((String) eventValues.getIndexedValues().get(1).getValue());
        curveData.setOwner(admin);
        curveData.setTransferOwnershipDeadline(deadline);
        isDirty = true;
        return HandleResult.genHandleSuccess();
    }

    protected HandleResult handleEventNewAdmin(String[] topics, String data, HandleEventExtraData handleEventExtraData) {
        log.info("{}:{} handleEventNewAdmin:{}, {}, {}", address, type, handleEventExtraData.getUniqueId());
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
        NewCurvePoolData curveData = this.getVarNewCurvePoolData(handleEventExtraData.getUniqueId());
        curveData.setOwner(admin);
        curveData.setTransferOwnershipDeadline(BigInteger.ZERO);
        isDirty = true;
        return HandleResult.genHandleSuccess();
    }

    protected HandleResult handleEventNewFeeConverter(String[] topics, String data, HandleEventExtraData handleEventExtraData) {
        log.info("{}:{} handleEventNewFeeConverter:{}, {}, {}", address, type, handleEventExtraData.getUniqueId());
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
        NewCurvePoolData curveData = this.getVarNewCurvePoolData(handleEventExtraData.getUniqueId());
        curveData.setFeeConverter(feeConverter);
        isDirty = true;
        return HandleResult.genHandleSuccess();
    }

    protected HandleResult handleEventCommitNewFee(String[] topics, String data, HandleEventExtraData handleEventExtraData) {
        log.info("{}:{} handleEventCommitNewFee:{}, {}, {}", address, type, handleEventExtraData.getUniqueId());
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
        NewCurvePoolData curveData = this.getVarNewCurvePoolData(handleEventExtraData.getUniqueId());
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
        log.info("{}:{} handleEventNewFee:{}, {}, {}", address, type, handleEventExtraData.getUniqueId());
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
        NewCurvePoolData curveData = this.getVarNewCurvePoolData(handleEventExtraData.getUniqueId());
        BigInteger fee = (BigInteger) eventValues.getNonIndexedValues().get(0).getValue();
        BigInteger adminFee = (BigInteger) eventValues.getNonIndexedValues().get(1).getValue();
        curveData.setFee(fee);
        curveData.setAdminFee(adminFee);
        curveData.setAdminActionsDeadline(BigInteger.ZERO);
        isDirty = true;
        return HandleResult.genHandleSuccess();
    }

    protected HandleResult handleEventRampA(String[] topics, String data, HandleEventExtraData handleEventExtraData) {
        log.info("{}:{} handleEventRampA:{}, {}, {}", address, type, handleEventExtraData.getUniqueId());
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
        NewCurvePoolData curveData = this.getVarNewCurvePoolData(handleEventExtraData.getUniqueId());
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
        log.info("{}:{} handleEventStopRampA:{}, {}, {}", address, type, handleEventExtraData.getUniqueId());
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
        NewCurvePoolData curveData = this.getVarNewCurvePoolData(handleEventExtraData.getUniqueId());
        BigInteger a = (BigInteger) eventValues.getNonIndexedValues().get(0).getValue();
        BigInteger aTime = (BigInteger) eventValues.getNonIndexedValues().get(1).getValue();
        curveData.setFutureA(a);
        curveData.setInitialA(BigInteger.valueOf(a.longValue()));
        curveData.setInitialATime(aTime);
        curveData.setInitialATime(BigInteger.valueOf(aTime.longValue()));
        isDirty = true;
        return HandleResult.genHandleSuccess();
    }

    public BigInteger[] removeLiquidity(String uniqueId, BigInteger _amount, BigInteger[] _minAmounts, long timestamp, IContractsHelper iContractsHelper) throws Exception {
        NewCurvePoolData data = this.getVarNewCurvePoolData(uniqueId);
        BigInteger lpTotalSupply = data.getLpTotalSupply();
        BigInteger[] amounts = empty(coinsCount);
        for (int i = 0; i < coinsCount; i++) {
            BigInteger oldBalance = data.getBalances()[i];
            BigInteger value = oldBalance.multiply(_amount).divide(lpTotalSupply);
            if (value.compareTo(_minAmounts[i]) < 0) {
                throw new Exception("Withdrawal resulted in fewer coins than expected");
            }
            data.updateBalances(i, oldBalance.subtract(value));
            amounts[i] = value;
        }
        data.setLpTotalSupply(lpTotalSupply.subtract(_amount));
        return amounts;
    }

    public BigInteger removeLiquidityImBalance(String uniqueId, BigInteger[] _amounts, BigInteger _minBurnAmount, long timestamp, IContractsHelper iContractsHelper) throws Exception {
        NewCurvePoolData poolData = this.getVarNewCurvePoolData(uniqueId);
        BigInteger tokenSupply = poolData.getLpTotalSupply();
        if (tokenSupply.compareTo(BigInteger.ZERO) == 0) {
            throw new Exception("zero total supply");
        }
        BigInteger amp = a(uniqueId, timestamp);
        BigInteger vpRate = vpRate(uniqueId, timestamp, iContractsHelper);
        BigInteger[] old_balances = copyBigIntegerArray(poolData.getBalances());
        BigInteger[] new_balances = copyBigIntegerArray(poolData.getBalances());
        BigInteger D0 = getDMem(vpRate, old_balances, amp);
        for (int i = 0; i < coinsCount; i++) {
            new_balances[i] = new_balances[i].subtract(_amounts[i]);
        }
        BigInteger D1 = getDMem(vpRate, new_balances, amp);
        BigInteger _fee = poolData.getFee().multiply(BigInteger.valueOf(coinsCount))
                .divide(BigInteger.valueOf((coinsCount - 1) * 4));

        BigInteger _admin_fee = poolData.getAdminFee();
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
            BigInteger D2 = getDMem(vpRate, new_balances, amp);
            token_amount = (D0.subtract(D2)).multiply(tokenSupply).divide(D0);
            if (token_amount.compareTo(BigInteger.ZERO) == 0) {
                throw new Exception("zero tokens burned");
            }

            token_amount = token_amount.add(BigInteger.ONE);
        }
        if (token_amount.compareTo(_minBurnAmount) > 0) {
            throw new Exception("Slippage screwed you");
        }

        poolData.setLpTotalSupply(tokenSupply.subtract(token_amount));
        return token_amount;
    }

    public BigInteger exchangeUnderlying(String uniqueId, int i, int j, BigInteger _dx, BigInteger mindy, long timestamp, IContractsHelper iContractsHelper) throws Exception {
        NewCurvePoolData data = this.getVarNewCurvePoolData(uniqueId);
        BigInteger[] rates = this.copyBigIntegerArray(this.rates);
        String basePool = data.getBasePool();
        int maxCoin = coinsCount - 1;
        rates[maxCoin] = vpRate(uniqueId, timestamp, iContractsHelper);
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
                AbstractCurve curve = (AbstractCurve) iContractsHelper.getContract(basePool);
                dxWFee = curve.addLiquidity(uniqueId, baseIputs, BigInteger.ZERO, timestamp, iContractsHelper);
                x = dxWFee.multiply(rates[maxCoin]).divide(PRECISION);
                x = x.add(xp[maxCoin]);
            }
            BigInteger y = getY(uniqueId, metaI, metaJ, x, xp, timestamp);
            dy = xp[metaJ].subtract(y).subtract(BigInteger.ONE);
            BigInteger dyFee = dy.multiply(data.getFee()).divide(FEE_DENOMINATOR);
            dy = dy.subtract(dyFee).multiply(PRECISION).divide(rates[metaJ]);
            BigInteger dyAdminFee = dyFee.multiply(data.getAdminFee()).divide(FEE_DENOMINATOR);
            dyAdminFee = dyAdminFee.multiply(PRECISION).divide(rates[metaJ]);
            BigInteger balanceI = data.getBalances()[metaI].add(dxWFee);
            BigInteger balanceJ = data.getBalances()[metaJ].subtract(dy).subtract(dyAdminFee);
            data.updateBalances(metaI, balanceI);
            data.updateBalances(metaJ, balanceJ);

            if (baseJ >= 0) {
                AbstractCurve curve = (AbstractCurve) iContractsHelper.getContract(basePool);
                dy = curve.removeLiquidityOneCoin(uniqueId, dy, baseJ, BigInteger.ZERO, timestamp, iContractsHelper);
            }

            if (dy.compareTo(mindy) < 0) {
                throw new Exception("Too few coins in result");
            }
        } else {
            AbstractCurve curve = (AbstractCurve) iContractsHelper.getContract(basePool);
            dy = curve.exchange(uniqueId, baseI, baseJ, dxWFee, mindy, timestamp, iContractsHelper);
        }
        return dy;
    }

    private BigInteger getAmountWFee(int _tokenId, BigInteger dx) {
        // 查看CurvePoolV2 feeIndex 是USDT 不是特殊收费ERC20，之后需要在添加
        return dx;
    }

    public BigInteger a(String uniqueId, long timestamp) {
        NewCurvePoolData data = this.getVarNewCurvePoolData(uniqueId);
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

    private BigInteger[] xp(String uniqueId, BigInteger vpRate) {
        NewCurvePoolData data = this.getVarNewCurvePoolData(uniqueId);
        BigInteger[] result = copyBigIntegerArray(this.rates);
        result[coinsCount - 1] = vpRate;
        BigInteger[] balances = data.getBalances();
        for (int i = 0; i < coinsCount; i++) {
            result[i] = result[i].multiply(balances[i]).divide(PRECISION);
        }
        return result;
    }

    private BigInteger[] xpMem(BigInteger vpRate, BigInteger[] balances) {
        BigInteger[] result = copyBigIntegerArray(this.rates);
        result[coinsCount - 1] = vpRate;
        for (int i = 0; i < coinsCount; i++) {
            result[i] = result[i].multiply(balances[i]).divide(PRECISION);
        }
        return result;
    }

    private BigInteger vpRate(String uniqueID, long timeStamp, IContractsHelper iContractsHelper) {
        NewCurvePoolData data = this.getVarNewCurvePoolData(uniqueID);
        try {
            if (timeStamp > data.getBaseCacheUpdated().longValue() + BASE_CACHE_EXPIRES) {
                AbstractCurve curve = ((AbstractCurve) iContractsHelper.getContract(data.getBasePool()));
                BigInteger price = curve.getVirtualPrice(uniqueID, 0, iContractsHelper);

                data.setBaseVirtualPrice(price);
                data.setBaseCacheUpdated(BigInteger.valueOf(timeStamp));
            }
        } catch (Exception e) {

        }
        return data.getBaseVirtualPrice();
    }

    private BigInteger vpRateRo(String uniqueID, long timeStamp, IContractsHelper iContractsHelper) {
        NewCurvePoolData data = this.getVarNewCurvePoolData(uniqueID);
        try {
            if (timeStamp > data.getBaseCacheUpdated().longValue() + BASE_CACHE_EXPIRES) {
                AbstractCurve curve = (AbstractCurve) iContractsHelper.getContract(data.getBasePool());
                return curve.getVirtualPrice(uniqueID, 0, iContractsHelper);
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

    public BigInteger getVirtualPrice(String uniqueId, long timestamp, IContractsHelper iContractsHelper) throws Exception {
        NewCurvePoolData data = this.getVarNewCurvePoolData(uniqueId);
        BigInteger amp = a(uniqueId, timestamp);
        BigInteger vpRate = vpRateRo(uniqueId, timestamp, iContractsHelper);
        BigInteger[] xp = xp(uniqueId, vpRate);
        BigInteger d = getD(xp, amp);
        BigInteger totalSupply = data.getLpTotalSupply();
        return d.multiply(PRECISION).divide(totalSupply);
    }

    public BigInteger calcTokenAmount(String uniqueId, long timestamp, BigInteger[] amounts, boolean deposit, IContractsHelper iContractsHelper) throws Exception {
        NewCurvePoolData data = this.getVarNewCurvePoolData(uniqueId);
        BigInteger[] balances = data.getCopyBalances();
        BigInteger amp = a(uniqueId, timestamp);
        BigInteger vpRate = vpRateRo(uniqueId, timestamp, iContractsHelper);
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

    public BigInteger getY(String uniqueId, int i, int j, BigInteger x, BigInteger[] xp_, long timestamp) throws Exception {
        if (i == j) {
            throw new Exception("same coin");
        }
        if (j >= coinsCount) {
            throw new Exception("j above N_COINS");
        }

        if (i >= coinsCount) {
            throw new Exception("i above N_COINS");
        }
        BigInteger amp = a(uniqueId, timestamp);
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

    public BigInteger getDy(String uniqueId, int i, int j, BigInteger dx, long timestamp, IContractsHelper iContractsHelper) throws Exception {
        NewCurvePoolData data = this.getVarNewCurvePoolData(uniqueId);
        BigInteger[] rates = copyBigIntegerArray(this.rates);
        rates[coinsCount - 1] = vpRateRo(uniqueId, timestamp, iContractsHelper);
        BigInteger[] xp = xp(uniqueId, rates[coinsCount - 1]);
        BigInteger x = xp[i].add(dx.multiply(rates[i]).divide(PRECISION));
        BigInteger y = getY(uniqueId, i, j, x, xp, timestamp);
        BigInteger dy = xp[j].subtract(y).subtract(BigInteger.ONE);
        BigInteger fee = data.getFee().multiply(dy).divide(FEE_DENOMINATOR);
        return dy.subtract(fee).multiply(PRECISION).divide(rates[j]);
    }

    private BigInteger[] empty(int num) {
        BigInteger[] res = new BigInteger[num];
        for (int i = 0; i < num; i++) {
            res[i] = BigInteger.ZERO;
        }
        return res;
    }

    private BigInteger[] copyBigIntegerArray(BigInteger[] src) {
        BigInteger[] res = new BigInteger[src.length];
        for (int i = 0; i < src.length; i++) {
            res[i] = new BigInteger(src[i].toString());
        }
        return res;
    }

    @Override
    public BigInteger getDyUnderlying(String uniqueId, int i, int j, BigInteger _dx, long timestamp, IContractsHelper iContractsHelper) throws Exception {
        NewCurvePoolData data = this.getVarNewCurvePoolData(uniqueId);
        int maxCoin = coinsCount - 1;
        BigInteger vpRate = vpRateRo(uniqueId, timestamp, iContractsHelper);
        BigInteger[] xp = xp(uniqueId, vpRate);
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
                AbstractCurve curve = (AbstractCurve) iContractsHelper.getContract(data.getBasePool());
                x = (curve).calcTokenAmount(uniqueId, timestamp, baseInput, true, iContractsHelper).multiply(vpRate).divide(PRECISION);
                x = x.subtract(x.multiply(curve.fee()).divide(BigInteger.TWO.multiply(FEE_DENOMINATOR)));
                x = x.add(xp[maxCoin]);
            } else {
                AbstractCurve curve = (AbstractCurve) iContractsHelper.getContract(data.getBasePool());
                return curve.getDy(uniqueId, baseI, baseJ, _dx, timestamp, iContractsHelper);
            }
        }

        BigInteger y = getY(uniqueId, metaI, metaJ, x, xp, timestamp);
        BigInteger dy = xp[metaJ].subtract(y).subtract(BigInteger.ONE);
        dy = dy.subtract(data.getFee().multiply(dy).divide(FEE_DENOMINATOR));
        if (baseJ < 0) {
            dy = dy.divide(precisions[metaJ]);
        } else {
            AbstractCurve curve = (AbstractCurve) iContractsHelper.getContract(data.getBasePool());
            dy = curve.calcWithdrawOneCoin(uniqueId, timestamp, dy.multiply(PRECISION).divide(vpRate), baseJ, iContractsHelper);
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

    private BigInteger[] localCalcWithdrawOneCoin(String uniqueId, BigInteger _token_amount, int i, BigInteger vpRate, long timestamp, NewCurvePoolData data) throws Exception {
        BigInteger nCoins = BigInteger.valueOf(coinsCount);
        BigInteger amp = a(uniqueId, timestamp);
        BigInteger[] xp = xp(uniqueId, vpRate);
        BigInteger d0 = getD(xp, amp);
        BigInteger totalSupply = data.getLpTotalSupply();
        BigInteger d1 = d0.subtract(_token_amount.multiply(d0).divide(totalSupply));
        BigInteger newY = getYD(amp, i, xp, d1);
        BigInteger _fee = data.getFee().multiply(nCoins).divide(BigInteger.valueOf((coinsCount - 1) * 4));

        BigInteger[] rates = copyBigIntegerArray(this.rates);
        rates[coinsCount - 1] = vpRate;
        BigInteger[] xpReduced = xp(uniqueId, vpRate);
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

    public BigInteger calcWithdrawOneCoin(String uniqueId, long timestamp, BigInteger _token_amount, int i, IContractsHelper iContractsHelper) throws Exception {
        BigInteger vpRate = vpRateRo(uniqueId, timestamp, iContractsHelper);
        BigInteger[] res = localCalcWithdrawOneCoin(uniqueId, _token_amount, i, vpRate, timestamp, this.getVarNewCurvePoolData(uniqueId));
        return res[0];
    }

    @Data
    public static class ContractExtraData {
        private int coinsCount;
        private int baseCoinsCount;
        private BigInteger[] rates;
        private BigInteger[] precisionMul;
        private String poolName;
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
