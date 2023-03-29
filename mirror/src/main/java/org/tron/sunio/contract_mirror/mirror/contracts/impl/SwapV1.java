package org.tron.sunio.contract_mirror.mirror.contracts.impl;

import cn.hutool.core.util.ObjectUtil;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.tron.sunio.contract_mirror.mirror.chainHelper.IChainHelper;
import org.tron.sunio.contract_mirror.mirror.consts.ContractMirrorConst;
import org.tron.sunio.contract_mirror.mirror.contracts.BaseContract;
import org.tron.sunio.contract_mirror.mirror.contracts.IContractsHelper;
import org.tron.sunio.contract_mirror.mirror.contracts.factory.SwapFactoryV1;
import org.tron.sunio.contract_mirror.mirror.dao.SwapFactoryV1Data;
import org.tron.sunio.contract_mirror.mirror.dao.SwapV1Data;
import org.tron.sunio.contract_mirror.mirror.enums.ContractType;
import org.tron.sunio.contract_mirror.mirror.tools.CallContractUtil;
import org.web3j.abi.EventValues;

import java.math.BigInteger;
import java.util.Map;

import static org.tron.sunio.contract_mirror.event_decode.events.SwapV1Event.EVENT_NAME_ADD_LIQUIDITY;
import static org.tron.sunio.contract_mirror.event_decode.events.SwapV1Event.EVENT_NAME_ADD_LIQUIDITY_BODY;
import static org.tron.sunio.contract_mirror.event_decode.events.SwapV1Event.EVENT_NAME_ADMIN_FEE_MINT;
import static org.tron.sunio.contract_mirror.event_decode.events.SwapV1Event.EVENT_NAME_REMOVE_LIQUIDITY;
import static org.tron.sunio.contract_mirror.event_decode.events.SwapV1Event.EVENT_NAME_REMOVE_LIQUIDITY_BODY;
import static org.tron.sunio.contract_mirror.event_decode.events.SwapV1Event.EVENT_NAME_SNAPSHOT;
import static org.tron.sunio.contract_mirror.event_decode.events.SwapV1Event.EVENT_NAME_TOKEN_PURCHASE;
import static org.tron.sunio.contract_mirror.event_decode.events.SwapV1Event.EVENT_NAME_TOKEN_PURCHASE_BODY;
import static org.tron.sunio.contract_mirror.event_decode.events.SwapV1Event.EVENT_NAME_TOKEN_TO_TOKEN;
import static org.tron.sunio.contract_mirror.event_decode.events.SwapV1Event.EVENT_NAME_TRANSFER;
import static org.tron.sunio.contract_mirror.event_decode.events.SwapV1Event.EVENT_NAME_TRANSFER_BODY;
import static org.tron.sunio.contract_mirror.event_decode.events.SwapV1Event.EVENT_NAME_TRX_PURCHASE;
import static org.tron.sunio.contract_mirror.mirror.consts.ContractMirrorConst.EMPTY_ADDRESS;
import static org.tron.sunio.contract_mirror.mirror.consts.ContractMirrorConst.EMPTY_TOPIC_VALUE;
import static org.tron.sunio.contract_mirror.mirror.consts.ContractMirrorConst.METHOD_BALANCE;
import static org.tron.sunio.contract_mirror.mirror.consts.ContractMirrorConst.METHOD_DECIMALS;
import static org.tron.sunio.contract_mirror.mirror.consts.ContractMirrorConst.METHOD_K_LAST;
import static org.tron.sunio.contract_mirror.mirror.consts.ContractMirrorConst.METHOD_NAME;
import static org.tron.sunio.contract_mirror.mirror.consts.ContractMirrorConst.METHOD_SYMBOL;
import static org.tron.sunio.contract_mirror.mirror.consts.ContractMirrorConst.METHOD_TOKEN;
import static org.tron.sunio.contract_mirror.mirror.consts.ContractMirrorConst.METHOD_TONE_BALANCE;
import static org.tron.sunio.contract_mirror.mirror.consts.ContractMirrorConst.METHOD_TOTAL_SUPPLY;
import static org.tron.sunio.contract_mirror.mirror.consts.ContractMirrorConst.SWAP_V1_NO_FEE;
import static org.tron.sunio.contract_mirror.mirror.consts.ContractMirrorConst.V1_VERSION;

@Slf4j
public class SwapV1 extends BaseContract {

    private String tokenAddress;
    private String factory;
    @Setter
    private SwapV1Data swapV1Data;

    public SwapV1(String factory, String address, IChainHelper iChainHelper, IContractsHelper iContractsHelper, String tokenAddress,
                  final Map<String, String> sigMap) {
        super(address, ContractType.SWAP_V1, V1_VERSION, iChainHelper, iContractsHelper, sigMap);
        this.tokenAddress = tokenAddress;
        this.factory = factory;
    }

    public SwapV1(SwapV1Data data, IChainHelper iChainHelper, IContractsHelper iContractsHelper, final Map<String, String> sigMap) {
        this(data.getFactory(), data.getAddress(), iChainHelper, iContractsHelper, data.getTokenAddress(), sigMap);
        this.setSwapV1Data(data);
    }

    private SwapV1Data getVarSwapV1Data() {
        if (ObjectUtil.isNull(swapV1Data)) {
            swapV1Data = new SwapV1Data();
            swapV1Data.setFactory(factory);
            swapV1Data.setType(this.type);
            swapV1Data.setAddress(this.address);
            swapV1Data.setVersion(version);
            swapV1Data.setTokenAddress(this.tokenAddress);
            swapV1Data.setUsing(true);
        }
        return swapV1Data;
    }

    public SwapV1Data getSwapV1Data() {
        return getVarSwapV1Data().copySelf();
    }

    @Override
    public BaseContract copySelf() {
        try {
            rlock.lock();
            SwapV1Data data = this.getSwapV1Data();
            SwapV1 v1 = new SwapV1(data, iChainHelper, iContractsHelper, sigMap);
            v1.setReady(this.isReady);
            v1.setAddExchangeContracts(this.isAddExchangeContracts);
            v1.setUsing(this.isUsing);
            v1.setDirty(this.isDirty);
            return v1;
        } finally {
            rlock.unlock();
        }
    }

    @Override
    public boolean initDataFromChain1() {
        SwapV1Data v1Data = this.getVarSwapV1Data();
        String name = CallContractUtil.getString(iChainHelper, ContractMirrorConst.EMPTY_ADDRESS, address, "name");
        String symbol = CallContractUtil.getString(iChainHelper, ContractMirrorConst.EMPTY_ADDRESS, address, "symbol");
        String tokenName = CallContractUtil.getString(iChainHelper, ContractMirrorConst.EMPTY_ADDRESS, tokenAddress, "name");
        String tokenSymbol = CallContractUtil.getString(iChainHelper, ContractMirrorConst.EMPTY_ADDRESS, tokenAddress, "symbol");
        long decimals = CallContractUtil.getU256(iChainHelper, ContractMirrorConst.EMPTY_ADDRESS, address, "decimals").longValue();
        long tokenDecimals = CallContractUtil.getU256(iChainHelper, ContractMirrorConst.EMPTY_ADDRESS, tokenAddress, "decimals").longValue();
        BigInteger lpTotalSupply = CallContractUtil.getU256(iChainHelper, ContractMirrorConst.EMPTY_ADDRESS, address, "totalSupply");
        BigInteger tokenBalance = CallContractUtil.tokenBalance(iChainHelper, this.getAddress(), tokenAddress);
        BigInteger trxBalance = getBalance(address);
        BigInteger kLast = CallContractUtil.getU256(iChainHelper, ContractMirrorConst.EMPTY_ADDRESS, address, "kLast");
        isReady = false;
        v1Data.setName(name);
        v1Data.setSymbol(symbol);
        v1Data.setDecimals(decimals);
        v1Data.setTrxBalance(trxBalance);
        v1Data.setLpTotalSupply(lpTotalSupply);
        v1Data.setKLast(kLast);
        v1Data.setTokenDecimals(tokenDecimals);
        v1Data.setTokenBalance(tokenBalance);
        v1Data.setTokenName(tokenName);
        v1Data.setTokenSymbol(tokenSymbol);
        v1Data.setReady(isReady);
        isDirty = true;
        return true;
    }

    @Override
    public void updateBaseInfo(boolean isUsing, boolean isReady, boolean isAddExchangeContracts) {
        SwapV1Data v1Data = this.getVarSwapV1Data();
        v1Data.setReady(isReady);
        v1Data.setUsing(isUsing);
        v1Data.setAddExchangeContracts(isAddExchangeContracts);
        isDirty = true;
    }

    @Override
    protected void saveUpdateToCache() {

    }

    @Override
    protected HandleResult handleEvent1(String eventName, String[] topics, String data, HandleEventExtraData handleEventExtraData) {
        HandleResult result;
        switch (eventName) {
            case EVENT_NAME_TRANSFER:
                result = handleEventTransfer(topics, data, handleEventExtraData);
                break;
            case EVENT_NAME_TOKEN_PURCHASE:
                result = handleTokenPurchase(topics, data, handleEventExtraData);
                break;
            case EVENT_NAME_TRX_PURCHASE:
                result = handleTrxPurchase(topics, data, handleEventExtraData);
                break;
            case EVENT_NAME_TOKEN_TO_TOKEN:
                result = handleTokenToToken(topics, data);
                break;
            case EVENT_NAME_SNAPSHOT:
                result = handleEventSnapshot(topics, data, handleEventExtraData);
                break;
            case EVENT_NAME_ADD_LIQUIDITY:
                result = handleAddLiquidity(topics, data, handleEventExtraData);
                break;
            case EVENT_NAME_REMOVE_LIQUIDITY:
                result = handleRemoveLiquidity(topics, data, handleEventExtraData);
                break;
            case EVENT_NAME_ADMIN_FEE_MINT:
                result = handleAdminFeeMint(topics, data);
                break;
            default:
                log.warn("Contract:{} type:{} event:{}  unique id:{} not handle", address, type, topics[0], handleEventExtraData.getUniqueId());
                result = HandleResult.genHandleFailMessage(String.format("Event:%s not handle", handleEventExtraData.getUniqueId()));
                break;
        }
        return result;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getStatus() {
        return (T) getVarSwapV1Data();
    }

    @Override
    public String getVersion() {
        return version;
    }

    /*
    *  String METHOD_SYMBOL = "symbol";
    String METHOD_K_LAST = "kLast";
    String METHOD_TOTAL_SUPPLY = "totalSupply";
    *
    * */
    @Override
    @SuppressWarnings("unchecked")
    public <T> T handleSpecialRequest(String method, String params) throws Exception {
        switch (method) {
            case METHOD_NAME:
                return (T) this.getVarSwapV1Data().getName();
            case METHOD_DECIMALS:
                return (T) (Long) this.getVarSwapV1Data().getDecimals();
            case METHOD_SYMBOL:
                return (T) this.getVarSwapV1Data().getSymbol();
            case METHOD_K_LAST:
                return (T) this.getVarSwapV1Data().getKLast();
            case METHOD_TOTAL_SUPPLY:
                return (T) this.getVarSwapV1Data().getLpTotalSupply();
            case METHOD_TOKEN:
                return (T) this.getVarSwapV1Data().getTokenAddress();
            case METHOD_BALANCE:
                return (T) this.getVarSwapV1Data().getTrxBalance();
            case METHOD_TONE_BALANCE:
                return (T) this.getVarSwapV1Data().getTokenBalance();
        }
        return null;
    }

    private HandleResult handleEventTransfer(String[] topics, String data, HandleEventExtraData handleEventExtraData) {
        log.info("SwapV1:{}, handleEventTransfer, topics:{} data:{} ", address, topics, data);
        EventValues eventValues = getEventValue(EVENT_NAME_TRANSFER, EVENT_NAME_TRANSFER_BODY, topics, data,
                handleEventExtraData.getUniqueId());
        if (ObjectUtil.isNull(eventValues)) {
            return HandleResult.genHandleFailMessage(String.format("Contract%s, type:%s decode handleEventTransfer fail!, unique id :%s",
                    address, type, handleEventExtraData.getUniqueId()));
        }
        SwapV1Data v1Data = this.getVarSwapV1Data();
        String from = (String) eventValues.getIndexedValues().get(0).getValue();
        String to = (String) eventValues.getIndexedValues().get(1).getValue();
        BigInteger amount = (BigInteger) eventValues.getNonIndexedValues().get(0).getValue();
        boolean change = false;
        if (to.equalsIgnoreCase(EMPTY_TOPIC_VALUE)) {
            v1Data.setLpTotalSupply(v1Data.getLpTotalSupply().subtract(amount));
            change = true;

        }
        if (from.equalsIgnoreCase(EMPTY_TOPIC_VALUE)) {
            v1Data.setLpTotalSupply(v1Data.getLpTotalSupply().add(amount));
            change = true;

        }
        if (change) {
            isDirty = true;
        }
        return HandleResult.genHandleSuccess();
    }

    private HandleResult handleEventSnapshot(String[] topics, String data, HandleEventExtraData handleEventExtraData) {
        log.info("SwapV1:{}, handleEventSnapshot, topics:{} data:{} ", address, topics, data);
//        EventValues eventValues = getEventValue(EVENT_NAME_SNAPSHOT, EVENT_NAME_SNAPSHOT_BODY, topics, data,
//                handleEventExtraData.getUniqueId());
//        if (ObjectUtil.isNull(eventValues)) {
//            return HandleResult.genHandleFailMessage(String.format("Contract%s, type:%s decode handleEventSnapshot fail!, unique id :%s",
//                    address, type, handleEventExtraData.getUniqueId()));
//        }
//        SwapV1Data v1Data = this.getVarSwapV1Data();
//        BigInteger trx = (BigInteger) eventValues.getIndexedValues().get(1).getValue();
//        BigInteger tokenBalance = (BigInteger) eventValues.getIndexedValues().get(2).getValue();
//        v1Data.setTokenBalance(tokenBalance);
//        v1Data.setTrxBalance(trx);
//        isDirty = true;
        return HandleResult.genHandleSuccess();
    }

    private HandleResult handleAddLiquidity(String[] topics, String data, HandleEventExtraData handleEventExtraData) {
        log.info("SwapV1:{}, handleAddLiquidity, topics:{} data:{} ", address, topics, data);
        EventValues eventValues = getEventValue(EVENT_NAME_ADD_LIQUIDITY, EVENT_NAME_ADD_LIQUIDITY_BODY, topics, data,
                handleEventExtraData.getUniqueId());
        if (ObjectUtil.isNull(eventValues)) {
            return HandleResult.genHandleFailMessage(String.format("Contract%s, type:%s decode handleEventSnapshot fail!, unique id :%s",
                    address, type, handleEventExtraData.getUniqueId()));
        }
        SwapV1Data v1Data = this.getVarSwapV1Data();
        BigInteger trx = (BigInteger) eventValues.getIndexedValues().get(1).getValue();
        BigInteger tokenBalance = (BigInteger) eventValues.getIndexedValues().get(2).getValue();
        BigInteger trxNew = v1Data.getTrxBalance().add(trx);
        BigInteger tokenBalanceNew = v1Data.getTokenBalance().add(tokenBalance);
        if (isFeeOn(v1Data)) {
            BigInteger kLast = tokenBalanceNew.multiply(tokenBalanceNew).sqrt();
            v1Data.setKLast(kLast);
        }
        v1Data.setTrxBalance(trxNew);
        v1Data.setTokenBalance(tokenBalanceNew);
        isDirty = true;
        return HandleResult.genHandleSuccess();
    }

    private HandleResult handleRemoveLiquidity(String[] topics, String data, HandleEventExtraData handleEventExtraData) {
        log.info("SwapV1:{}, handleRemoveLiquidity, topics:{} data:{} ", address, topics, data);
        EventValues eventValues = getEventValue(EVENT_NAME_REMOVE_LIQUIDITY, EVENT_NAME_REMOVE_LIQUIDITY_BODY, topics, data,
                handleEventExtraData.getUniqueId());
        if (ObjectUtil.isNull(eventValues)) {
            return HandleResult.genHandleFailMessage(String.format("Contract%s, type:%s decode handleEventSnapshot fail!, unique id :%s",
                    address, type, handleEventExtraData.getUniqueId()));
        }
        SwapV1Data v1Data = this.getVarSwapV1Data();
        BigInteger trx = (BigInteger) eventValues.getIndexedValues().get(1).getValue();
        BigInteger tokenBalance = (BigInteger) eventValues.getIndexedValues().get(2).getValue();
        BigInteger trxNew = v1Data.getTrxBalance().subtract(trx);
        BigInteger tokenBalanceNew = v1Data.getTokenBalance().subtract(tokenBalance);
        if (isFeeOn(v1Data)) {
            BigInteger kLast = tokenBalanceNew.multiply(tokenBalanceNew).sqrt();
            v1Data.setKLast(kLast);
        }
        v1Data.setTrxBalance(trxNew);
        v1Data.setTokenBalance(tokenBalanceNew);
        isDirty = true;
        return HandleResult.genHandleSuccess();
    }

    private HandleResult handleAdminFeeMint(String[] topics, String data) {
        log.info("handleAdminFeeMint not implements!");
        return HandleResult.genHandleFailMessage("handleAdminFeeMint not implements!");
    }

    private HandleResult handleTokenPurchase(String[] topics, String data, HandleEventExtraData handleEventExtraData) {
        log.info("SwapV1:{}, handleTokenPurchase, topics:{} data:{} ", address, topics, data);
        EventValues eventValues = getEventValue(EVENT_NAME_TOKEN_PURCHASE, EVENT_NAME_TOKEN_PURCHASE_BODY, topics, data,
                handleEventExtraData.getUniqueId());
        if (ObjectUtil.isNull(eventValues)) {
            return HandleResult.genHandleFailMessage(String.format("Contract%s, type:%s decode handleEventSnapshot fail!, unique id :%s",
                    address, type, handleEventExtraData.getUniqueId()));
        }
        SwapV1Data v1Data = this.getVarSwapV1Data();
        BigInteger trxIn = (BigInteger) eventValues.getIndexedValues().get(1).getValue();
        BigInteger tokenOut = (BigInteger) eventValues.getIndexedValues().get(2).getValue();
        BigInteger trxNew = v1Data.getTrxBalance().add(trxIn);
        BigInteger tokenBalanceNew = v1Data.getTokenBalance().subtract(tokenOut);
        v1Data.setTrxBalance(trxNew);
        v1Data.setTokenBalance(tokenBalanceNew);
        isDirty = true;
        return HandleResult.genHandleSuccess();
    }

    private HandleResult handleTrxPurchase(String[] topics, String data, HandleEventExtraData handleEventExtraData) {
        log.info("SwapV1:{}, handleTrxPurchase, topics:{} data:{} ", address, topics, data);
        EventValues eventValues = getEventValue(EVENT_NAME_TOKEN_PURCHASE, EVENT_NAME_TOKEN_PURCHASE_BODY, topics, data,
                handleEventExtraData.getUniqueId());
        if (ObjectUtil.isNull(eventValues)) {
            return HandleResult.genHandleFailMessage(String.format("Contract%s, type:%s decode handleEventSnapshot fail!, unique id :%s",
                    address, type, handleEventExtraData.getUniqueId()));
        }
        SwapV1Data v1Data = this.getVarSwapV1Data();
        BigInteger tokenIn = (BigInteger) eventValues.getIndexedValues().get(1).getValue();
        BigInteger trxOut = (BigInteger) eventValues.getIndexedValues().get(2).getValue();
        BigInteger trxNew = v1Data.getTrxBalance().subtract(trxOut);
        BigInteger tokenBalanceNew = v1Data.getTokenBalance().add(tokenIn);
        v1Data.setTrxBalance(trxNew);
        v1Data.setTokenBalance(tokenBalanceNew);
        isDirty = true;
        return HandleResult.genHandleSuccess();
    }

    private HandleResult handleTokenToToken(String[] topics, String data) {
        log.info("TokenToToken not implements!");
        return HandleResult.genHandleFailMessage("handleTokenToToken not implements!");
    }

    private boolean isFeeOn(SwapV1Data v1Data) {
        boolean feeOn = false;
        BaseContract baseContract = this.iContractsHelper.getContract(v1Data.getFactory());
        if (ObjectUtil.isNotNull(baseContract) && (baseContract instanceof SwapFactoryV1)) {
            SwapFactoryV1Data factoryV1Data = ((SwapFactoryV1) baseContract).getSwapFactoryV1Data();
            feeOn = !(factoryV1Data.getFeeTo().equalsIgnoreCase(EMPTY_ADDRESS));
        }
        return feeOn;
    }

    // return  liquidity amount Ps:not change data
    public BigInteger addLiquidity(BigInteger trxAmount, BigInteger minLiquidity, BigInteger maxTokens,
                                   SwapV1Data swapV1Data) throws Exception {
        if (minLiquidity.compareTo(BigInteger.ZERO) <= 0) {
            throw new Exception("minLiquidity must greater than 0");
        }
        boolean feeOn = false;
        BigInteger liquidityMinted;
        BigInteger totalLiquidity = new BigInteger(swapV1Data.getLpTotalSupply().toString());
        if (totalLiquidity.compareTo(BigInteger.ZERO) > 0) {
            BigInteger trxReserve = swapV1Data.getTrxBalance();
            BigInteger tokenReserve = swapV1Data.getTokenBalance();
            BigInteger tokenAmount = (trxAmount.multiply(tokenReserve).divide(trxReserve)).add(BigInteger.ONE);
            liquidityMinted = trxAmount.multiply(totalLiquidity).divide(trxReserve);
            if (maxTokens.compareTo(tokenAmount) < 0 || liquidityMinted.compareTo(minLiquidity) < 0) {
                throw new Exception("max tokens not meet or liquidityMinted not meet minLiquidity");
            }
            feeOn = mintFee(trxReserve, tokenReserve, swapV1Data);
            swapV1Data.setTrxBalance(trxReserve.add(trxAmount));
            swapV1Data.setTokenBalance(tokenReserve.add(tokenAmount));
            swapV1Data.setLpTotalSupply(totalLiquidity.add(liquidityMinted));

        } else {
            swapV1Data.setTokenBalance(maxTokens);
            swapV1Data.setTrxBalance(swapV1Data.getTrxBalance().add(trxAmount));
            swapV1Data.setLpTotalSupply(swapV1Data.getTrxBalance());
            BaseContract baseContract = this.iContractsHelper.getContract(swapV1Data.getFactory());
            if (ObjectUtil.isNotNull(baseContract) && (baseContract instanceof SwapFactoryV1)) {
                SwapFactoryV1Data factoryV1Data = ((SwapFactoryV1) baseContract).getSwapFactoryV1Data();
                feeOn = !(factoryV1Data.getFeeTo().equalsIgnoreCase(EMPTY_ADDRESS));
            }
            liquidityMinted = swapV1Data.getTrxBalance();
        }
        if (feeOn && swapV1Data.getKLast().compareTo(BigInteger.ZERO) > 0) {
            // 默认线上合约都是有值的，0值是针对老版本的v1 不包含kLast
            swapV1Data.setKLast(swapV1Data.getTrxBalance().multiply(swapV1Data.getTokenBalance()));
        }
        return liquidityMinted;
    }

    // return [trx, tokenBalance] Ps:not change data
    public BigInteger[] removeLiquidity(BigInteger amount, BigInteger minTrx, BigInteger minTokens,
                                        SwapV1Data v1Data) throws Exception {
        if (amount.compareTo(BigInteger.ZERO) <= 0
                || minTrx.compareTo(BigInteger.ZERO) <= 0 || minTokens.compareTo(BigInteger.ZERO) <= 0) {
            throw new Exception("illegal input parameters");
        }

        BigInteger totalLiquidity = new BigInteger(v1Data.getLpTotalSupply().toString());
        if (totalLiquidity.compareTo(BigInteger.ZERO) <= 0) {
            throw new Exception("total_liquidity must greater than 0");
        }
        BigInteger tokenReserve = v1Data.getTokenBalance();
        BigInteger trxReserve = v1Data.getTrxBalance();
        boolean feeOn = mintFee(trxReserve, tokenReserve, v1Data);

        BigInteger trxAmount = amount.multiply(trxReserve).divide(totalLiquidity);
        BigInteger tokenAmount = amount.multiply(tokenReserve).divide(totalLiquidity);
        if (trxAmount.compareTo(minTrx) < 0 || tokenAmount.compareTo(minTokens) < 0) {
            throw new Exception("minToken or minTrx not meet");
        }

        v1Data.setTrxBalance(v1Data.getTrxBalance().subtract(trxAmount));
        v1Data.setTokenBalance(v1Data.getTokenBalance().subtract(tokenAmount));
        v1Data.setLpTotalSupply(v1Data.getLpTotalSupply().subtract(amount));

        if (feeOn && swapV1Data.getKLast().compareTo(BigInteger.ZERO) > 0) {
            // 默认线上合约都是有值的，0值是针对老版本的v1 不包含kLast
            v1Data.setKLast(v1Data.getTrxBalance().multiply(v1Data.getTokenBalance()));
        }

        return new BigInteger[]{trxAmount, tokenAmount};
    }

    public BigInteger trxToTokenInput(BigInteger trxSold, BigInteger minToken, SwapV1Data swapV1Data) throws Exception {
        BigInteger tokenBought = getInputPrice(trxSold, swapV1Data.getTrxBalance(), swapV1Data.getTokenBalance());
        if (tokenBought.compareTo(minToken) < 0) {
            throw new Exception("tokensBought < minToken");
        }
        swapV1Data.setTrxBalance(swapV1Data.getTrxBalance().add(trxSold));
        swapV1Data.setTokenBalance(swapV1Data.getTokenBalance().subtract(tokenBought));
        return tokenBought;
    }

    public BigInteger trxToTokenOutput(BigInteger tokenBought, BigInteger maxTrx, SwapV1Data swapV1Data) throws Exception {
        BigInteger trxSold = getOutputPrice(tokenBought, swapV1Data.getTrxBalance(), swapV1Data.getTokenBalance());
        if (trxSold.compareTo(maxTrx) < 0) {
            throw new Exception("trxSold < maxTrx");
        }
        swapV1Data.setTrxBalance(swapV1Data.getTrxBalance().add(trxSold));
        swapV1Data.setTokenBalance(swapV1Data.getTokenBalance().subtract(tokenBought));
        return trxSold;
    }

    public BigInteger tokenToTrxInput(BigInteger tokenSold, BigInteger minToken, SwapV1Data swapV1Data) throws Exception {
        BigInteger trxBought = getInputPrice(tokenSold, swapV1Data.getTokenBalance(), swapV1Data.getTrxBalance());
        if (trxBought.compareTo(minToken) < 0) {
            throw new Exception("trxBought < minToken");
        }
        swapV1Data.setTrxBalance(swapV1Data.getTrxBalance().subtract(trxBought));
        swapV1Data.setTokenBalance(swapV1Data.getTokenBalance().add(tokenSold));
        return trxBought;
    }

    public BigInteger tokenToTrxOutput(BigInteger trxBought, BigInteger maxTokens, SwapV1Data swapV1Data) throws Exception {
        BigInteger tokenSold = getOutputPrice(trxBought, swapV1Data.getTokenBalance(), swapV1Data.getTrxBalance());
        if (tokenSold.compareTo(maxTokens) < 0) {
            throw new Exception("tokensSold < maxTokens");
        }
        swapV1Data.setTrxBalance(swapV1Data.getTrxBalance().subtract(trxBought));
        swapV1Data.setTokenBalance(swapV1Data.getTokenBalance().add(tokenSold));
        return tokenSold;
    }

    public BigInteger tokenToTokenInput(BigInteger tokensSold, BigInteger minTokensBought, BigInteger minTrxBought,
                                        String exchangeAddress, SwapV1Data swapV1Data) throws Exception {
        if (exchangeAddress.equals(this.address) || exchangeAddress.equals(EMPTY_ADDRESS)) {
            throw new Exception("illegal exchange addr");
        }
        BigInteger trxBought = tokenToTrxInput(tokensSold, minTrxBought, swapV1Data);
        BaseContract baseContract = this.iContractsHelper.getContract(exchangeAddress);
        if (ObjectUtil.isNull(baseContract)) {
            throw new Exception(String.format("Get no %s contract instance", exchangeAddress));
        }
        if (baseContract instanceof SwapV1) {
            SwapV1 swapV1 = (SwapV1) baseContract;

            BigInteger tokensBought = swapV1.trxToTokenInput(trxBought, minTokensBought, swapV1Data);
            return tokensBought;
        } else {
            throw new Exception(String.format("Get %s contract instance not SwapV1"));
        }
    }

    public BigInteger tokenToTokenOutput(BigInteger tokensBought, BigInteger maxTokenSold, BigInteger minTrxSold,
                                         String exchangeAddress, SwapV1Data swapV1Data) throws Exception {
        if (exchangeAddress.equals(this.address) || exchangeAddress.equals(EMPTY_ADDRESS)) {
            throw new Exception("illegal exchange addr");
        }
        BaseContract baseContract = this.iContractsHelper.getContract(exchangeAddress);
        if (ObjectUtil.isNull(baseContract)) {
            throw new Exception(String.format("Get no %s contract instance", exchangeAddress));
        }
        if (baseContract instanceof SwapV1) {
            SwapV1 swapV1 = (SwapV1) baseContract;

            BigInteger trxBought = swapV1.trxToTokenOutput(tokensBought, minTrxSold, swapV1Data);
            BigInteger tokenSold = trxToTokenOutput(trxBought, maxTokenSold, swapV1Data);
            return tokenSold;
        } else {
            throw new Exception(String.format("Get %s contract instance not SwapV1"));
        }
    }

    public boolean mintFee(BigInteger reserve0, BigInteger reserve1, SwapV1Data swapV1Data) {
        boolean feeOn = false;
        BaseContract baseContract = this.iContractsHelper.getContract(swapV1Data.getFactory());
        if (ObjectUtil.isNull(baseContract)) {
            log.warn("{} get factory contract:{} fail", this.address, swapV1Data.getFactory());
            return feeOn;
        }
        if (baseContract instanceof SwapFactoryV1) {
            SwapFactoryV1Data factoryV1Data = ((SwapFactoryV1) baseContract).getSwapFactoryV1Data();
            feeOn = !(factoryV1Data.getFeeTo().equalsIgnoreCase(EMPTY_ADDRESS));
            BigInteger feeToRate = BigInteger.valueOf(factoryV1Data.getFeeToRate());
            BigInteger kLast = swapV1Data.getKLast();
            if (feeOn) {
                if (kLast.compareTo(BigInteger.ZERO) != 0 && feeToRate.compareTo(SWAP_V1_NO_FEE) != 0) {
                    BigInteger rootK = reserve0.multiply(reserve1).sqrt();
                    BigInteger rootKLast = kLast.sqrt();
                    if (rootK.compareTo(rootKLast) > 0) {
                        BigInteger numerator = swapV1Data.getLpTotalSupply().multiply(rootK.subtract(rootKLast));
                        BigInteger denominator = rootK.multiply(feeToRate).add(rootKLast);
                        if (denominator.compareTo(BigInteger.ZERO) > 0) {
                            BigInteger liquidity = numerator.divide(denominator);
                            if (liquidity.compareTo(BigInteger.ZERO) > 0) {
                                swapV1Data.setLpTotalSupply(swapV1Data.getLpTotalSupply().add(liquidity));
                            }
                        }
                    }
                }
            } else {
                swapV1Data.setKLast(BigInteger.ZERO);
            }
        } else {
            log.warn("{} get factory contract:{} not a SwapFactoryV1 instance ", this.address, swapV1Data.getFactory());

        }
        return feeOn;

    }

    // x*y=(x-a)*(y+b)
    //  => b=y*a/(x-a)
    public static BigInteger getOutputPrice(BigInteger outputAmount, BigInteger inputReserve,
                                            BigInteger outputReserve) throws Exception {
        if (outputAmount.compareTo(BigInteger.ZERO) <= 0 || inputReserve.compareTo(BigInteger.ZERO) <= 0) {
            throw new Exception("Wrong input");
        }
        BigInteger numerator = inputReserve.multiply(outputAmount).multiply(BigInteger.valueOf(1000));
        BigInteger denominator = (outputReserve.subtract(outputAmount)).multiply(BigInteger.valueOf(997));
        return (numerator.divide(denominator)).add(BigInteger.ONE);
    }

    // x*y=(x-a)*(y+b)
    // => a=x*b/(y+b)
    public static BigInteger getInputPrice(BigInteger inputAmount, BigInteger inputReserve,
                                           BigInteger outputReserve) throws Exception {

        if (inputReserve.compareTo(BigInteger.ZERO) <= 0 || outputReserve.compareTo(BigInteger.ZERO) <= 0) {
            throw new Exception("Wrong input");
        }

        BigInteger input_amount_with_fee = inputAmount.multiply(BigInteger.valueOf(997));
        BigInteger numerator = input_amount_with_fee.multiply(outputReserve);
        BigInteger denominator = inputReserve.multiply(BigInteger.valueOf(1000)).add(input_amount_with_fee);
        return numerator.divide(denominator);
    }


}
