package org.tron.sunio.contract_mirror.mirror.contracts.impl;

import cn.hutool.core.util.ObjectUtil;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.tron.sunio.contract_mirror.mirror.chainHelper.IChainHelper;
import org.tron.sunio.contract_mirror.mirror.chainHelper.TriggerContractInfo;
import org.tron.sunio.contract_mirror.mirror.consts.ContractMirrorConst;
import org.tron.sunio.contract_mirror.mirror.contracts.BaseContract;
import org.tron.sunio.contract_mirror.mirror.contracts.IContractsHelper;
import org.tron.sunio.contract_mirror.mirror.contracts.factory.SwapFactoryV2;
import org.tron.sunio.contract_mirror.mirror.dao.SwapFactoryV2Data;
import org.tron.sunio.contract_mirror.mirror.dao.SwapV2PairData;
import org.tron.sunio.contract_mirror.mirror.enums.ContractType;
import org.tron.sunio.contract_mirror.mirror.tools.CallContractUtil;
import org.web3j.abi.EventValues;
import org.web3j.abi.TypeReference;
import org.web3j.abi.datatypes.Type;
import org.web3j.abi.datatypes.generated.Uint112;
import org.web3j.abi.datatypes.generated.Uint32;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.tron.sunio.contract_mirror.event_decode.events.SwapV2PairEvent.EVENT_NAME_NEW_BURN_BODY;
import static org.tron.sunio.contract_mirror.event_decode.events.SwapV2PairEvent.EVENT_NAME_NEW_MINT_BODY;
import static org.tron.sunio.contract_mirror.event_decode.events.SwapV2PairEvent.EVENT_NAME_NEW_SWAP_BODY;
import static org.tron.sunio.contract_mirror.event_decode.events.SwapV2PairEvent.EVENT_NAME_TRANSFER_BODY;
import static org.tron.sunio.contract_mirror.event_decode.events.SwapV2PairEvent.EVENT_NAME_NEW_BURN;
import static org.tron.sunio.contract_mirror.event_decode.events.SwapV2PairEvent.EVENT_NAME_NEW_MINT;
import static org.tron.sunio.contract_mirror.event_decode.events.SwapV2PairEvent.EVENT_NAME_NEW_SWAP;
import static org.tron.sunio.contract_mirror.event_decode.events.SwapV2PairEvent.EVENT_NAME_NEW_SYNC;
import static org.tron.sunio.contract_mirror.event_decode.events.SwapV2PairEvent.EVENT_NAME_TRANSFER;
import static org.tron.sunio.contract_mirror.mirror.consts.ContractMirrorConst.EMPTY_ADDRESS;
import static org.tron.sunio.contract_mirror.mirror.consts.ContractMirrorConst.EMPTY_TOPIC_VALUE;
import static org.tron.sunio.contract_mirror.mirror.consts.ContractMirrorConst.METHOD_BALANCE;
import static org.tron.sunio.contract_mirror.mirror.consts.ContractMirrorConst.METHOD_DECIMALS;
import static org.tron.sunio.contract_mirror.mirror.consts.ContractMirrorConst.METHOD_FACTORY;
import static org.tron.sunio.contract_mirror.mirror.consts.ContractMirrorConst.METHOD_GET_RESERVES;
import static org.tron.sunio.contract_mirror.mirror.consts.ContractMirrorConst.METHOD_K_LAST;
import static org.tron.sunio.contract_mirror.mirror.consts.ContractMirrorConst.METHOD_NAME;
import static org.tron.sunio.contract_mirror.mirror.consts.ContractMirrorConst.METHOD_PRICE0_CUMULATIVE_LAST;
import static org.tron.sunio.contract_mirror.mirror.consts.ContractMirrorConst.METHOD_PRICE1_CUMULATIVE_LAST;
import static org.tron.sunio.contract_mirror.mirror.consts.ContractMirrorConst.METHOD_SYMBOL;
import static org.tron.sunio.contract_mirror.mirror.consts.ContractMirrorConst.METHOD_TOKEN0;
import static org.tron.sunio.contract_mirror.mirror.consts.ContractMirrorConst.METHOD_TOKEN1;
import static org.tron.sunio.contract_mirror.mirror.consts.ContractMirrorConst.METHOD_TOTAL_SUPPLY;
import static org.tron.sunio.contract_mirror.mirror.consts.ContractMirrorConst.V2_VERSION;

@Slf4j
public class SwapV2Pair extends BaseContract {
    private static final BigInteger Q112 = BigInteger.TWO.pow(112);
    private static final BigInteger MINIMUM_LIQUIDITY = BigInteger.TEN.pow(3);
    private String factory;
    @Setter
    private SwapV2PairData swapV2PairData;

    public SwapV2Pair(String address, String factory, IChainHelper iChainHelper, IContractsHelper iContractsHelper,
                      Map<String, String> sigMap) {
        super(address, ContractType.SWAP_V2_PAIR, iChainHelper, iContractsHelper, sigMap);
        this.factory = factory;
    }

    public SwapV2PairData getSwapV2PairData() {
        return getVarSwapV2PairData().copySelf();
    }

    private SwapV2PairData getVarSwapV2PairData() {
        if (ObjectUtil.isNull(swapV2PairData)) {
            swapV2PairData = new SwapV2PairData();
            swapV2PairData.setFactory(factory);
            swapV2PairData.setType(type);
            swapV2PairData.setAddress(address);
            swapV2PairData.setUsing(true);
        }
        return swapV2PairData;
    }

    private void callReservesOnChain(SwapV2PairData swapV2PairData) {
        //getReserves()
        try {
            List<Type> inputParameters = new ArrayList<>();
            List<TypeReference<?>> outputParameters = new ArrayList<>();
            outputParameters.add(new TypeReference<Uint112>() {
            });
            outputParameters.add(new TypeReference<Uint112>() {
            });
            outputParameters.add(new TypeReference<Uint32>() {
            });
            TriggerContractInfo triggerContractInfo = new TriggerContractInfo(
                    ContractMirrorConst.EMPTY_ADDRESS,
                    this.getAddress(),
                    "getReserves",
                    inputParameters,
                    outputParameters
            );
            List<Type> results = this.iChainHelper.triggerConstantContract(triggerContractInfo);
            if (results.size() != 3) {
                log.error("SwapV2Pair :{} fail to get getReserves, size:{}", address, results.size());
                return;
            }
            swapV2PairData.setReserve0((BigInteger) results.get(0).getValue());
            swapV2PairData.setReserve1((BigInteger) results.get(1).getValue());
            swapV2PairData.setBlockTimestampLast(((BigInteger) results.get(2).getValue()).longValue());
        } catch (Exception e) {
            log.error("SwapV2Pair :{} fail to get getReserves, size:{}", address, e.toString());
        }
    }

    @Override
    public boolean initDataFromChain1() {
        SwapV2PairData swapV2PairData = this.getVarSwapV2PairData();
        String token0 = CallContractUtil.getTronAddress(iChainHelper, ContractMirrorConst.EMPTY_ADDRESS, address, "token0");
        swapV2PairData.setToken0(token0);
        String token1 = CallContractUtil.getTronAddress(iChainHelper, ContractMirrorConst.EMPTY_ADDRESS, address, "token1");
        swapV2PairData.setToken1(token1);
        callReservesOnChain(swapV2PairData);
        swapV2PairData.setPrice0CumulativeLast(CallContractUtil.getU256(iChainHelper, ContractMirrorConst.EMPTY_ADDRESS, address, "price0CumulativeLast"));
        swapV2PairData.setPrice1CumulativeLast(CallContractUtil.getU256(iChainHelper, ContractMirrorConst.EMPTY_ADDRESS, address, "price1CumulativeLast"));
        String name = CallContractUtil.getString(iChainHelper, ContractMirrorConst.EMPTY_ADDRESS, address, "name");
        swapV2PairData.setName(name);
        String symbol = CallContractUtil.getString(iChainHelper, ContractMirrorConst.EMPTY_ADDRESS, address, "symbol");
        swapV2PairData.setSymbol(symbol);
        String token0Name = CallContractUtil.getString(iChainHelper, ContractMirrorConst.EMPTY_ADDRESS, token0, "name");
        swapV2PairData.setToken0Name(token0Name);
        String token0Symbol = CallContractUtil.getString(iChainHelper, ContractMirrorConst.EMPTY_ADDRESS, token0, "symbol");
        swapV2PairData.setToken0Symbol(token0Symbol);
        String token1Name = CallContractUtil.getString(iChainHelper, ContractMirrorConst.EMPTY_ADDRESS, token1, "name");
        swapV2PairData.setToken1Name(token1Name);
        String token1Symbol = CallContractUtil.getString(iChainHelper, ContractMirrorConst.EMPTY_ADDRESS, token1, "symbol");
        swapV2PairData.setToken1Symbol(token1Symbol);
        long decimals = CallContractUtil.getU256(iChainHelper, ContractMirrorConst.EMPTY_ADDRESS, address, "decimals").longValue();
        swapV2PairData.setDecimals(decimals);
        long token0Decimals = CallContractUtil.getU256(iChainHelper, ContractMirrorConst.EMPTY_ADDRESS, token0, "decimals").longValue();
        swapV2PairData.setToken0Decimals(token0Decimals);
        long token1Decimals = CallContractUtil.getU256(iChainHelper, ContractMirrorConst.EMPTY_ADDRESS, token1, "decimals").longValue();
        swapV2PairData.setToken1Decimals(token1Decimals);
        BigInteger kLast = CallContractUtil.getU256(iChainHelper, ContractMirrorConst.EMPTY_ADDRESS, address, "kLast");
        swapV2PairData.setKLast(kLast);
        BigInteger lpTotalSupply = CallContractUtil.getU256(iChainHelper, ContractMirrorConst.EMPTY_ADDRESS, address, "totalSupply");
        swapV2PairData.setLpTotalSupply(lpTotalSupply);
        BigInteger trxBalance = getBalance(address);
        swapV2PairData.setTrxBalance(trxBalance);
        isDirty = true;
        return true;
    }

    @Override
    public void updateBaseInfo(boolean isUsing, boolean isReady, boolean isAddExchangeContracts) {
        SwapV2PairData swapV2PairData = this.getVarSwapV2PairData();
        swapV2PairData.setUsing(isUsing);
        swapV2PairData.setReady(isReady);
        swapV2PairData.setAddExchangeContracts(isAddExchangeContracts);
        isDirty = true;
    }

    @Override
    public String getVersion() {
        return V2_VERSION;
    }

    @Override
    protected void saveUpdateToCache() {
    }

    @Override
    protected HandleResult handleEvent1(String eventName, String[] topics, String data, HandleEventExtraData handleEventExtraData) {
        HandleResult result;
        switch (eventName) {
            case EVENT_NAME_TRANSFER:
                result = handleTransfer(topics, data, handleEventExtraData);
                break;
            case EVENT_NAME_NEW_MINT:
                result = handleMint(topics, data, handleEventExtraData);
                break;
            case EVENT_NAME_NEW_BURN:
                result = handleBurn(topics, data, handleEventExtraData);
                break;
            case EVENT_NAME_NEW_SWAP:
                result = handleSwap(topics, data, handleEventExtraData);
                break;
            case EVENT_NAME_NEW_SYNC:
                result = handleSync(topics, data, handleEventExtraData);
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
        return (T) getVarSwapV2PairData();
    }

    @Override
    public <T> T handleSpecialRequest(String method, String params) throws Exception {
        switch (method) {
            case METHOD_NAME:
                return (T) this.getVarSwapV2PairData().getName();
            case METHOD_DECIMALS:
                return (T) (Long) this.getVarSwapV2PairData().getDecimals();
            case METHOD_SYMBOL:
                return (T) this.getVarSwapV2PairData().getSymbol();
            case METHOD_K_LAST:
                return (T) this.getVarSwapV2PairData().getKLast();
            case METHOD_TOTAL_SUPPLY:
                return (T) this.getVarSwapV2PairData().getLpTotalSupply();
            case METHOD_BALANCE:
                return (T) this.getVarSwapV2PairData().getTrxBalance();
            case METHOD_FACTORY:
                return (T) this.getVarSwapV2PairData().getFactory();
            case METHOD_TOKEN0:
                return (T) this.getVarSwapV2PairData().getToken0();
            case METHOD_TOKEN1:
                return (T) this.getVarSwapV2PairData().getToken1();
            case METHOD_PRICE0_CUMULATIVE_LAST:
                return (T) this.getVarSwapV2PairData().getPrice0CumulativeLast();
            case METHOD_PRICE1_CUMULATIVE_LAST:
                return (T) this.getVarSwapV2PairData().getPrice1CumulativeLast();
            case METHOD_GET_RESERVES:
                return (T) this.getVarSwapV2PairData().getReserves();
        }
        return null;
    }

    private HandleResult handleTransfer(String[] topics, String data, HandleEventExtraData handleEventExtraData) {
        log.info("SwapV1:{}, handleTransfer, topics:{} data:{} ", address, topics, data);
        EventValues eventValues = getEventValue(EVENT_NAME_TRANSFER, EVENT_NAME_TRANSFER_BODY, topics, data,
                handleEventExtraData.getUniqueId());
        if (ObjectUtil.isNull(eventValues)) {
            return HandleResult.genHandleFailMessage(String.format("Contract%s, type:%s decode handleTransfer fail!, unique id :%s",
                    address, type, handleEventExtraData.getUniqueId()));
        }
        SwapV2PairData swapV2PairData = this.getVarSwapV2PairData();
        String from = (String) eventValues.getIndexedValues().get(0).getValue();
        String to = (String) eventValues.getIndexedValues().get(1).getValue();
        BigInteger amount = (BigInteger) eventValues.getNonIndexedValues().get(0).getValue();
        boolean change = false;
        if (to.equalsIgnoreCase(EMPTY_TOPIC_VALUE)) {
            swapV2PairData.setLpTotalSupply(swapV2PairData.getLpTotalSupply().subtract(amount));
            change = true;

        }
        if (from.equalsIgnoreCase(EMPTY_TOPIC_VALUE)) {
            swapV2PairData.setLpTotalSupply(swapV2PairData.getLpTotalSupply().add(amount));
            change = true;

        }
        if (change) {
            isDirty = true;
        }
        return HandleResult.genHandleSuccess();
    }

    private HandleResult handleMint(String[] topics, String data, HandleEventExtraData handleEventExtraData) {
        log.info("SwapV1:{}, handleMint, topics:{} data:{} ", address, topics, data);
        EventValues eventValues = getEventValue(EVENT_NAME_NEW_MINT, EVENT_NAME_NEW_MINT_BODY, topics, data,
                handleEventExtraData.getUniqueId());
        if (ObjectUtil.isNull(eventValues)) {
            return HandleResult.genHandleFailMessage(String.format("Contract%s, type:%s decode handleTransfer fail!, unique id :%s",
                    address, type, handleEventExtraData.getUniqueId()));
        }
        SwapV2PairData swapV2PairData = this.getVarSwapV2PairData();
        BigInteger amount0 = (BigInteger) eventValues.getNonIndexedValues().get(0).getValue();
        BigInteger amount1 = (BigInteger) eventValues.getNonIndexedValues().get(1).getValue();
        boolean feeOn = isFeeOn(swapV2PairData);
        BigInteger balance0 = swapV2PairData.getReserve0().add(amount0);
        BigInteger balance1 = swapV2PairData.getReserve1().add(amount1);
        update(balance0, balance1, handleEventExtraData.getTimeStamp(), swapV2PairData);
        if (feeOn) {
            BigInteger kLast = swapV2PairData.getReserve0().multiply(swapV2PairData.getReserve1());
            swapV2PairData.setKLast(kLast);
        }
        isDirty = true;
        return HandleResult.genHandleSuccess();
    }

    private HandleResult handleBurn(String[] topics, String data, HandleEventExtraData handleEventExtraData) {
        log.info("SwapV1:{}, handleBurn, topics:{} data:{} ", address, topics, data);
        EventValues eventValues = getEventValue(EVENT_NAME_NEW_BURN, EVENT_NAME_NEW_BURN_BODY, topics, data,
                handleEventExtraData.getUniqueId());
        if (ObjectUtil.isNull(eventValues)) {
            return HandleResult.genHandleFailMessage(String.format("Contract%s, type:%s decode handleTransfer fail!, unique id :%s",
                    address, type, handleEventExtraData.getUniqueId()));
        }
        SwapV2PairData swapV2PairData = this.getVarSwapV2PairData();
        BigInteger amount0 = (BigInteger) eventValues.getNonIndexedValues().get(0).getValue();
        BigInteger amount1 = (BigInteger) eventValues.getNonIndexedValues().get(1).getValue();
        boolean feeOn = isFeeOn(swapV2PairData);
        BigInteger balance0 = swapV2PairData.getReserve0().subtract(amount0);
        BigInteger balance1 = swapV2PairData.getReserve1().subtract(amount1);
        update(balance0, balance1, handleEventExtraData.getTimeStamp(), swapV2PairData);
        if (feeOn) {
            BigInteger kLast = swapV2PairData.getReserve0().multiply(swapV2PairData.getReserve1());
            swapV2PairData.setKLast(kLast);
        }
        isDirty = true;
        return HandleResult.genHandleSuccess();
    }

    private HandleResult handleSwap(String[] topics, String data, HandleEventExtraData handleEventExtraData) {
        log.info("SwapV1:{}, handleSwap, topics:{} data:{} ", address, topics, data);
        EventValues eventValues = getEventValue(EVENT_NAME_NEW_SWAP, EVENT_NAME_NEW_SWAP_BODY, topics, data,
                handleEventExtraData.getUniqueId());
        if (ObjectUtil.isNull(eventValues)) {
            return HandleResult.genHandleFailMessage(String.format("Contract%s, type:%s decode handleTransfer fail!, unique id :%s",
                    address, type, handleEventExtraData.getUniqueId()));
        }
        SwapV2PairData swapV2PairData = this.getVarSwapV2PairData();
        BigInteger amount0In = (BigInteger) eventValues.getNonIndexedValues().get(0).getValue();
        BigInteger amount1In = (BigInteger) eventValues.getNonIndexedValues().get(1).getValue();
        BigInteger amount0Out = (BigInteger) eventValues.getNonIndexedValues().get(2).getValue();
        BigInteger amount1Out = (BigInteger) eventValues.getNonIndexedValues().get(3).getValue();
        BigInteger balance0;
        BigInteger balance1;

        if (amount0In.compareTo(BigInteger.ZERO) > 0) {
            balance0 = swapV2PairData.getReserve0().add(amount0In);
            balance1 = swapV2PairData.getReserve1().subtract(amount1Out);
        } else {
            balance1 = swapV2PairData.getReserve1().add(amount1In);
            balance0 = swapV2PairData.getReserve0().subtract(amount0Out);
        }
        update(balance0, balance1, handleEventExtraData.getTimeStamp(), swapV2PairData);
        isDirty = true;
        return HandleResult.genHandleSuccess();
    }

    private HandleResult handleSync(String[] topics, String data, HandleEventExtraData handleEventExtraData) {
        log.info("SwapV1:{}, handleSync, topics:{} data:{} ", address, topics, data);
//        EventValues eventValues = getEventValue(EVENT_NAME_NEW_SYNC, EVENT_NAME_NEW_SYNC_BODY, topics, data,
//                handleEventExtraData.getUniqueId());
//        if (ObjectUtil.isNull(eventValues)) {
//            return HandleResult.genHandleFailMessage(String.format("Contract%s, type:%s decode handleSync fail!, unique id :%s",
//                    address, type, handleEventExtraData.getUniqueId()));
//        }
//        SwapV2PairData swapV2PairData = this.getVarSwapV2PairData();
//        BigInteger reserve0 = (BigInteger) eventValues.getNonIndexedValues().get(0).getValue();
//        BigInteger reserve1 = (BigInteger) eventValues.getNonIndexedValues().get(1).getValue();
//        //4294967296L = 2**32
//        long blockTimestampLast = handleEventExtraData.getTimeStamp() % 4294967296L;
//        long timeElapsed = blockTimestampLast - swapV2PairData.getBlockTimestampLast();
//        BigInteger reserve0Origin = swapV2PairData.getReserve0();
//        BigInteger reserve1Origin = swapV2PairData.getReserve1();
//        if (timeElapsed > 0 && reserve0Origin.compareTo(BigInteger.ZERO) != 0 && reserve1Origin.compareTo(BigInteger.ZERO) != 0) {
//            BigInteger price0Add = priceCumulativeLastAdd(reserve0Origin, reserve1Origin, timeElapsed);
//            BigInteger price1Add = priceCumulativeLastAdd(reserve1Origin, reserve0Origin, timeElapsed);
//            swapV2PairData.setPrice0CumulativeLast(swapV2PairData.getPrice0CumulativeLast().add(price0Add));
//            swapV2PairData.setPrice1CumulativeLast(swapV2PairData.getPrice1CumulativeLast().add(price1Add));
//        }
//        swapV2PairData.setReserve0(reserve0);
//        swapV2PairData.setReserve1(reserve1);
//        swapV2PairData.setBlockTimestampLast(blockTimestampLast);
//        isDirty = true;
        return HandleResult.genHandleSuccess();
    }

    public BigInteger[] burn(BigInteger lpTokenAmount, long timestamp) throws Exception {
        SwapV2PairData v2PairData = this.getSwapV2PairData();
        return burn(lpTokenAmount, timestamp, v2PairData);
    }

    public BigInteger[] burn(BigInteger lpAmount, long timestamp, SwapV2PairData swapV2PairData) throws Exception {
        BigInteger reserve0 = swapV2PairData.getReserve0();
        BigInteger reserve1 = swapV2PairData.getReserve1();
        boolean feeOn = mintFee(reserve0, reserve1, swapV2PairData);
        BigInteger lpTotalSupply = swapV2PairData.getLpTotalSupply();
        BigInteger amount0 = lpAmount.multiply(reserve0).divide(lpTotalSupply);
        BigInteger amount1 = lpAmount.multiply(reserve1).divide(lpTotalSupply);

        if (amount0.compareTo(BigInteger.ZERO) <= 0 || amount1.compareTo(BigInteger.ZERO) <= 0) {
            throw new Exception("UniswapV2: INSUFFICIENT_LIQUIDITY_BURNED");
        }

        _burn(lpAmount, swapV2PairData);
        BigInteger balance0 = reserve0.subtract(amount0);
        BigInteger balance1 = reserve1.subtract(amount1);
        update(balance0, balance1, timestamp, swapV2PairData);
        if (feeOn) {
            BigInteger kLast = swapV2PairData.getReserve0().multiply(swapV2PairData.getReserve1());
            swapV2PairData.setKLast(kLast);
        }
        return new BigInteger[]{amount0, amount1};
    }

    public BigInteger mint(BigInteger amount0, BigInteger amount1, long timeStamp) throws Exception {
        SwapV2PairData v2PairData = this.getSwapV2PairData();
        return mint(amount0, amount1, timeStamp, v2PairData);
    }

    public BigInteger mint(BigInteger amount0, BigInteger amount1, long timeStamp, SwapV2PairData swapV2PairData) throws Exception {
        BigInteger reserve0 = swapV2PairData.getReserve0();
        BigInteger reserve1 = swapV2PairData.getReserve1();
        boolean feeOn = mintFee(reserve0, reserve1, swapV2PairData);

        BigInteger liquidity;

        BigInteger lpTotalSupply = swapV2PairData.getLpTotalSupply();
        if (lpTotalSupply.compareTo(BigInteger.ZERO) == 0) {
            liquidity = amount0.multiply(amount1).sqrt().subtract(MINIMUM_LIQUIDITY);
            _mint(MINIMUM_LIQUIDITY, swapV2PairData);
        } else {
            liquidity = (amount0.multiply(lpTotalSupply).divide(reserve0)).min(
                    amount1.multiply(lpTotalSupply).divide(reserve1)
            );
        }
        _mint(liquidity, swapV2PairData);
        BigInteger balance0 = reserve0.add(amount0);
        BigInteger balance1 = reserve1.add(amount1);
        update(balance0, balance1, timeStamp, swapV2PairData);

        if (feeOn) {
            BigInteger kLast = swapV2PairData.getReserve0().multiply(swapV2PairData.getReserve1());
            swapV2PairData.setKLast(kLast);
        }
        return liquidity;
    }

    public void swap(BigInteger amountIn0, BigInteger amountIn1, BigInteger amountOut0, BigInteger amountOut1, long timestamp) throws Exception {
        SwapV2PairData v2PairData = this.getSwapV2PairData();
        swap(amountIn0, amountIn1, amountOut0, amountOut1, timestamp, v2PairData);
    }

    public void swap(BigInteger amountIn0, BigInteger amountIn1, BigInteger amountOut0, BigInteger amountOut1,
                     long timestamp, SwapV2PairData swapV2PairData) throws Exception {

        if (amountIn0.compareTo(BigInteger.ZERO) > 0 && amountIn1.compareTo(BigInteger.ZERO) > 0) {
            throw new Exception("Wrong Input");
        }

        if (amountOut0.compareTo(BigInteger.ZERO) > 0 && amountOut1.compareTo(BigInteger.ZERO) > 0) {
            throw new Exception("Wrong Input");
        }

        BigInteger reserve0 = swapV2PairData.getReserve0();
        BigInteger reserve1 = swapV2PairData.getReserve1();

        BigInteger balance0;
        BigInteger balance1;
        if (amountIn0.compareTo(BigInteger.ZERO) > 0) {
            balance0 = reserve0.add(amountIn0);
            balance1 = reserve1.subtract(amountOut1);
        } else {
            balance1 = reserve1.add(amountIn1);
            balance0 = reserve0.subtract(amountOut0);
        }

        update(balance0, balance1, timestamp, swapV2PairData);
    }

    private void update(BigInteger reserve0, BigInteger reserve1, long timeStamp, SwapV2PairData swapV2PairData) {
        long blockTimestampLast = timeStamp % 4294967296L;
        long timeElapsed = blockTimestampLast - swapV2PairData.getBlockTimestampLast();
        BigInteger reserve0Origin = swapV2PairData.getReserve0();
        BigInteger reserve1Origin = swapV2PairData.getReserve1();
        if (timeElapsed > 0 && reserve0Origin.compareTo(BigInteger.ZERO) != 0 && reserve1Origin.compareTo(BigInteger.ZERO) != 0) {
            BigInteger price0Add = priceCumulativeLastAdd(reserve0Origin, reserve1Origin, timeElapsed);
            BigInteger price1Add = priceCumulativeLastAdd(reserve1Origin, reserve0Origin, timeElapsed);
            swapV2PairData.setPrice0CumulativeLast(swapV2PairData.getPrice0CumulativeLast().add(price0Add));
            swapV2PairData.setPrice1CumulativeLast(swapV2PairData.getPrice1CumulativeLast().add(price1Add));
        }
        swapV2PairData.setReserve0(reserve0);
        swapV2PairData.setReserve1(reserve1);
        swapV2PairData.setBlockTimestampLast(blockTimestampLast);
    }

    private boolean isFeeOn(SwapV2PairData swapV2Data) {
        boolean feeOn = false;
        BaseContract baseContract = this.iContractsHelper.getContract(swapV2Data.getFactory());
        if (ObjectUtil.isNotNull(baseContract) && (baseContract instanceof SwapFactoryV2)) {
            SwapFactoryV2Data factoryV2Data = ((SwapFactoryV2) baseContract).getSwapFactoryV2Data();
            feeOn = !(factoryV2Data.getFeeTo().equalsIgnoreCase(EMPTY_ADDRESS));
        }
        return feeOn;
    }

    public boolean mintFee(BigInteger reserve0, BigInteger reserve1, SwapV2PairData swapV2Data) {
        boolean feeOn = isFeeOn(swapV2Data);
        BigInteger kLast = swapV2Data.getKLast();
        if (feeOn) {
            if (kLast.compareTo(BigInteger.ZERO) != 0) {
                BigInteger rootK = reserve0.multiply(reserve1).sqrt();
                BigInteger rootKLast = kLast.sqrt();
                if (rootK.compareTo(rootKLast) > 0) {
                    BigInteger numerator = swapV2Data.getLpTotalSupply().multiply(rootK.subtract(rootKLast));
                    BigInteger denominator = rootK.multiply(BigInteger.valueOf(5)).add(rootKLast);
                    if (denominator.compareTo(BigInteger.ZERO) > 0) {
                        BigInteger liquidity = numerator.divide(denominator);
                        if (liquidity.compareTo(BigInteger.ZERO) > 0) {
                            _mint(liquidity, swapV2Data);
                        }
                    }
                }
            }
        } else {
            swapV2Data.setKLast(BigInteger.ZERO);
        }
        return feeOn;
    }

    public void _mint(BigInteger value, SwapV2PairData swapV2Data) {
        swapV2PairData.setLpTotalSupply(swapV2Data.getLpTotalSupply().add(value));
    }

    public void _burn(BigInteger value, SwapV2PairData swapV2Data) {
        swapV2PairData.setLpTotalSupply(swapV2Data.getLpTotalSupply().subtract(value));
    }

    private BigInteger priceCumulativeLastAdd(BigInteger reserve0, BigInteger reserve1, long timeElapsed) {
        return reserve1.multiply(Q112).divide(reserve0).multiply(BigInteger.valueOf(timeElapsed));
    }

    /**
     * // given an input amount of an asset and pair reserves, returns the maximum output amount of the other asset
     * function getAmountOut(uint amountIn, uint reserveIn, uint reserveOut) internal pure returns (uint amountOut) {
     * require(amountIn > 0, 'UniswapV2Library: INSUFFICIENT_INPUT_AMOUNT');
     * require(reserveIn > 0 && reserveOut > 0, 'UniswapV2Library: INSUFFICIENT_LIQUIDITY');
     * uint amountInWithFee = amountIn.mul(997);
     * uint numerator = amountInWithFee.mul(reserveOut);
     * uint denominator = reserveIn.mul(1000).add(amountInWithFee);
     * amountOut = numerator / denominator;
     * }
     * x*y=(x-a)*(y+b)
     * => a=x*b/(y+b)
     */
    public BigInteger getAmountOut(String token0, String token1, BigInteger amountIn, SwapV2PairData v2PairData) throws Exception {
        if (BigInteger.ZERO.compareTo(amountIn) > 0) {
            throw new Exception("UniswapV2Library: INSUFFICIENT_INPUT_AMOUNT ");
        }
        BigInteger[] reserves = getSortedReverser(token0, token1, v2PairData);
        if (BigInteger.ZERO.compareTo(reserves[0]) > 0 ||
                BigInteger.ZERO.compareTo(reserves[1]) > 0) {
            throw new Exception("UniswapV2Library: INSUFFICIENT_LIQUIDITY ");
        }
        BigInteger amountInWithFee = amountIn.multiply(BigInteger.valueOf(997));
        BigInteger numerator = amountInWithFee.multiply(reserves[1]);
        BigInteger denominator = reserves[0].multiply(BigInteger.valueOf(1000)).add(amountInWithFee);
        return numerator.divide(denominator);
    }

    /**
     * // given an output amount of an asset and pair reserves, returns a required input amount of the other asset
     * function getAmountIn(uint amountOut, uint reserveIn, uint reserveOut) internal pure returns (uint amountIn) {
     * require(amountOut > 0, 'UniswapV2Library: INSUFFICIENT_OUTPUT_AMOUNT');
     * require(reserveIn > 0 && reserveOut > 0, 'UniswapV2Library: INSUFFICIENT_LIQUIDITY');
     * uint numerator = reserveIn.mul(amountOut).mul(1000);
     * uint denominator = reserveOut.sub(amountOut).mul(997);
     * amountIn = (numerator / denominator).add(1);
     * }
     * x*y=(x-a)*(y+b)
     * => b=y*a/(x-a)
     */

    public BigInteger getAmountIn(String token0, String token1, BigInteger amountOut, SwapV2PairData v2PairData) throws Exception {
        if (BigInteger.ZERO.compareTo(amountOut) > 0) {
            throw new Exception("UniswapV2Library: INSUFFICIENT_OUTPUT_AMOUNT ");
        }
        BigInteger[] reserves = getSortedReverser(token0, token1, v2PairData);
        if (BigInteger.ZERO.compareTo(reserves[0]) > 0 ||
                BigInteger.ZERO.compareTo(reserves[1]) > 0) {
            throw new Exception("UniswapV2Library: INSUFFICIENT_LIQUIDITY ");
        }
        BigInteger numerator = reserves[0].multiply(amountOut).multiply(BigInteger.valueOf(1000));
        BigInteger denominator = (reserves[1].subtract(amountOut)).multiply(BigInteger.valueOf(997));
        return numerator.divide(denominator);
    }

    public BigInteger[] getSortedReverser(String token0, String token1, SwapV2PairData v2PairData) throws Exception {
        BigInteger reserve0 = new BigInteger(v2PairData.getReserve0().toString());
        BigInteger reserve1 = new BigInteger(v2PairData.getReserve1().toString());
        if (token0.equals(v2PairData.getToken0()) && token1.equals(v2PairData.getToken1())) {
            return new BigInteger[]{reserve0, reserve1};
        }
        if (token1.equals(v2PairData.getToken0()) && token1.equals(v2PairData.getToken0())) {
            return new BigInteger[]{reserve1, reserve0};
        }
        throw new Exception("Wrong inout tokens");
    }

}
