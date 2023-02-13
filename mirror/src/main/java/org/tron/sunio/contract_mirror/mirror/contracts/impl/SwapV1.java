package org.tron.sunio.contract_mirror.mirror.contracts.impl;

import cn.hutool.core.util.ObjectUtil;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.tron.sunio.contract_mirror.mirror.chainHelper.IChainHelper;
import org.tron.sunio.contract_mirror.mirror.consts.ContractMirrorConst;
import org.tron.sunio.contract_mirror.mirror.contracts.BaseContract;
import org.tron.sunio.contract_mirror.mirror.contracts.IContractsHelper;
import org.tron.sunio.contract_mirror.mirror.dao.SwapV1Data;
import org.tron.sunio.contract_mirror.mirror.enums.ContractType;
import org.web3j.abi.EventValues;

import java.math.BigInteger;
import java.util.Map;

import static org.tron.sunio.contract_mirror.event_decode.events.SwapV1Event.EVENT_NAME_ADD_LIQUIDITY;
import static org.tron.sunio.contract_mirror.event_decode.events.SwapV1Event.EVENT_NAME_ADMIN_FEE_MINT;
import static org.tron.sunio.contract_mirror.event_decode.events.SwapV1Event.EVENT_NAME_REMOVE_LIQUIDITY;
import static org.tron.sunio.contract_mirror.event_decode.events.SwapV1Event.EVENT_NAME_SNAPSHOT;
import static org.tron.sunio.contract_mirror.event_decode.events.SwapV1Event.EVENT_NAME_SNAPSHOT_BODY;
import static org.tron.sunio.contract_mirror.event_decode.events.SwapV1Event.EVENT_NAME_TOKEN_PURCHASE;
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

@Slf4j
public class SwapV1 extends BaseContract {

    private String tokenAddress;
    @Getter
    @Setter
    private SwapV1Data swapV1Data;

    public SwapV1(String address, IChainHelper iChainHelper, IContractsHelper iContractsHelper, String tokenAddress,
                  final Map<String, String> sigMap) {
        super(address, ContractType.SWAP_V1, iChainHelper, iContractsHelper, sigMap);
        this.tokenAddress = tokenAddress;
    }

    private SwapV1Data getVarSwapV1Data() {
        if (ObjectUtil.isNull(swapV1Data)) {
            swapV1Data = new SwapV1Data();
            swapV1Data.setType(this.type);
            swapV1Data.setAddress(this.address);
            swapV1Data.setTokenAddress(this.tokenAddress);
            swapV1Data.setUsing(true);
        }
        return swapV1Data;
    }

    @Override
    public boolean initDataFromChain1() {
        SwapV1Data v1Data = this.getVarSwapV1Data();
        String name = callContractString(ContractMirrorConst.EMPTY_ADDRESS, "name");
        String symbol = callContractString(ContractMirrorConst.EMPTY_ADDRESS, "symbol");
        String tokenName = callContractString(ContractMirrorConst.EMPTY_ADDRESS, tokenAddress, "name");
        String tokenSymbol = callContractString(ContractMirrorConst.EMPTY_ADDRESS, tokenAddress, "symbol");
        long decimals = callContractU256(ContractMirrorConst.EMPTY_ADDRESS, "decimals").longValue();
        BigInteger kLast = callContractU256(ContractMirrorConst.EMPTY_ADDRESS, "kLast");
        BigInteger lpTotalSupply = callContractU256(ContractMirrorConst.EMPTY_ADDRESS, "totalSupply");
        BigInteger tokenBalance = tokenBalance(this.getAddress(), tokenAddress);
        BigInteger trxBalance = getBalance(address);
        isReady = false;
        v1Data.setName(name);
        v1Data.setSymbol(symbol);
        v1Data.setDecimals(decimals);
        v1Data.setKLast(kLast);
        v1Data.setTrxBalance(trxBalance);
        v1Data.setLpTotalSupply(lpTotalSupply);
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
                result = handleTokenPurchase(topics, data);
                break;
            case EVENT_NAME_TRX_PURCHASE:
                result = handleTrxPurchase(topics, data);
                break;
            case EVENT_NAME_TOKEN_TO_TOKEN:
                result = handleTokenToToken(topics, data);
                break;
            case EVENT_NAME_SNAPSHOT:
                result = handleEventSnapshot(topics, data, handleEventExtraData);
                break;
            case EVENT_NAME_ADD_LIQUIDITY:
                result = handleAddLiquidity(topics, data);
                break;
            case EVENT_NAME_REMOVE_LIQUIDITY:
                result = handleRemoveLiquidity(topics, data);
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
    public <T> T getStatus() {
        return (T) getVarSwapV1Data();
    }

    /*
    *  String METHOD_SYMBOL = "symbol";
    String METHOD_K_LAST = "kLast";
    String METHOD_TOTAL_SUPPLY = "totalSupply";
    *
    * */
    @Override
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
        EventValues eventValues = getEventValue(EVENT_NAME_SNAPSHOT, EVENT_NAME_SNAPSHOT_BODY, topics, data,
                handleEventExtraData.getUniqueId());
        if (ObjectUtil.isNull(eventValues)) {
            return HandleResult.genHandleFailMessage(String.format("Contract%s, type:%s decode handleEventSnapshot fail!, unique id :%s",
                    address, type, handleEventExtraData.getUniqueId()));
        }
        SwapV1Data v1Data = this.getVarSwapV1Data();
        BigInteger trx = (BigInteger) eventValues.getIndexedValues().get(1).getValue();
        BigInteger tokenBalance = (BigInteger) eventValues.getIndexedValues().get(2).getValue();
        v1Data.setTokenBalance(tokenBalance);
        v1Data.setTrxBalance(trx);
        isDirty = true;
        return HandleResult.genHandleSuccess();
    }

    private HandleResult handleAddLiquidity(String[] topics, String data) {
        log.info("handleAddLiquidity not implements!");
        return HandleResult.genHandleFailMessage("handleAddLiquidity not implements!");
    }

    private HandleResult handleRemoveLiquidity(String[] topics, String data) {
        log.info("handleRemoveLiquidity not implements!");
        return HandleResult.genHandleFailMessage("handleRemoveLiquidity not implements!");
    }

    private HandleResult handleAdminFeeMint(String[] topics, String data) {
        log.info("handleAdminFeeMint not implements!");
        return HandleResult.genHandleFailMessage("handleAdminFeeMint not implements!");
    }

    private HandleResult handleTokenPurchase(String[] topics, String data) {
        log.info("handleTokenPurchase not implements!");
        return HandleResult.genHandleFailMessage("handleTokenPurchase not implements!");
    }

    private HandleResult handleTrxPurchase(String[] topics, String data) {
        log.info("handleTrxPurchase not implements!");
        return HandleResult.genHandleFailMessage("handleTrxPurchase not implements!");
    }

    private HandleResult handleTokenToToken(String[] topics, String data) {
        log.info("TokenToToken not implements!");
        return HandleResult.genHandleFailMessage("handleTokenToToken not implements!");
    }

    // return  liquidity amount Ps:not change data
    public BigInteger addLiquidity(BigInteger trxAmount, BigInteger minLiquidity, BigInteger maxTokens,
                                   BigInteger deadline) throws Exception {
        if (minLiquidity.compareTo(BigInteger.ZERO) <= 0) {
            throw new Exception("minLiquidity must greater than 0");
        }
        SwapV1Data v1Data = this.getVarSwapV1Data().copySelf();
        BigInteger totalLiquidity = new BigInteger(v1Data.getLpTotalSupply().toString());
        if (totalLiquidity.compareTo(BigInteger.ZERO) > 0) {
            BigInteger trxReserve = v1Data.getTrxBalance();
            BigInteger tokenReserve = v1Data.getTokenBalance();
            BigInteger tokenAmount = (trxAmount.multiply(tokenReserve).divide(trxReserve)).add(BigInteger.ONE);
            BigInteger liquidityMinted = trxAmount.multiply(totalLiquidity).divide(trxReserve);
            if (maxTokens.compareTo(tokenAmount) < 0 || liquidityMinted.compareTo(minLiquidity) < 0) {
                throw new Exception("max tokens not meet or liquidityMinted not meet minLiquidity");
            }
            return liquidityMinted;
        } else {
            return v1Data.getTrxBalance();
        }
    }

    // return [trx, tokenBalance] Ps:not change data
    public BigInteger[] removeLiquidity(BigInteger amount, BigInteger minTrx, BigInteger minTokens,
                                        long deadline) throws Exception {
        if (amount.compareTo(BigInteger.ZERO) <= 0 || deadline < iContractsHelper.getBlockTime()
                || minTrx.compareTo(BigInteger.ZERO) <= 0 || minTokens.compareTo(BigInteger.ZERO) <= 0) {
            throw new Exception("illegal input parameters");
        }
        SwapV1Data v1Data = this.getVarSwapV1Data().copySelf();
        BigInteger totalLiquidity = new BigInteger(v1Data.getLpTotalSupply().toString());
        if (totalLiquidity.compareTo(BigInteger.ZERO) <= 0) {
            throw new Exception("total_liquidity must greater than 0");
        }

        BigInteger tokenReserve = v1Data.getTokenBalance();
        BigInteger trxReserve = v1Data.getTrxBalance();
        BigInteger trxAmount = amount.multiply(trxReserve).divide(totalLiquidity);
        BigInteger tokenAmount = amount.multiply(tokenReserve).divide(totalLiquidity);
        if (trxAmount.compareTo(minTrx) < 0 || tokenAmount.compareTo(minTokens) < 0) {
            throw new Exception("minToken or minTrx not meet");
        }
        return new BigInteger[]{trxAmount, tokenAmount};
    }

    public BigInteger trxToTokenInput(BigInteger trxSold, BigInteger minToken) throws Exception {
        SwapV1Data swapV1Data = this.getVarSwapV1Data().copySelf();
        BigInteger tokensBought = getInputPrice(trxSold, swapV1Data.getTrxBalance(), swapV1Data.getTokenBalance());
        if (tokensBought.compareTo(minToken) < 0) {
            throw new Exception("tokensBought < minToken");
        }
        return tokensBought;
    }

    public BigInteger trxToTokenOutput(BigInteger tokenBought, BigInteger maxTrx) throws Exception {
        SwapV1Data swapV1Data = this.getVarSwapV1Data().copySelf();
        BigInteger trxSold = getOutputPrice(tokenBought, swapV1Data.getTrxBalance(), swapV1Data.getTokenBalance());
        if (trxSold.compareTo(maxTrx) < 0) {
            throw new Exception("trxSold < maxTrx");
        }
        return trxSold;
    }

    public BigInteger tokenToTrxInput(BigInteger tokenSold, BigInteger minToken) throws Exception {
        SwapV1Data swapV1Data = this.getVarSwapV1Data().copySelf();
        BigInteger trxBought = getInputPrice(tokenSold, swapV1Data.getTokenBalance(), swapV1Data.getTrxBalance());
        if (trxBought.compareTo(minToken) < 0) {
            throw new Exception("trxBought < minToken");
        }
        return trxBought;
    }

    public BigInteger tokenToTrxOutput(BigInteger trxBought, BigInteger maxTokens) throws Exception {
        SwapV1Data swapV1Data = this.getVarSwapV1Data().copySelf();
        BigInteger tokensSold = getOutputPrice(trxBought, swapV1Data.getTokenBalance(), swapV1Data.getTrxBalance());
        if (tokensSold.compareTo(maxTokens) < 0) {
            throw new Exception("tokensSold < maxTokens");
        }
        return tokensSold;
    }

    public BigInteger tokenToTokenInput(BigInteger tokensSold, BigInteger minTokensBought, BigInteger minTrxBought,
                                        String exchangeAddress) throws Exception {
        if (exchangeAddress.equals(this.address) || exchangeAddress.equals(EMPTY_ADDRESS)) {
            throw new Exception("illegal exchange addr");
        }
        BigInteger trxBought = tokenToTrxInput(tokensSold, minTrxBought);
        BaseContract baseContract = this.iContractsHelper.getContract(exchangeAddress);
        if (ObjectUtil.isNull(baseContract)) {
            throw new Exception(String.format("Get no %s contract instance", exchangeAddress));
        }
        if (baseContract instanceof SwapV1) {
            SwapV1 swapV1 = (SwapV1) baseContract;

            BigInteger tokensBought = swapV1.trxToTokenInput(trxBought, minTokensBought);
            return tokensBought;
        } else {
            throw new Exception(String.format("Get %s contract instance not SwapV1"));
        }
    }

    public BigInteger tokenToTokenOutput(BigInteger tokensBought, BigInteger maxTokenSold, BigInteger minTrxSold,
                                         String exchangeAddress) throws Exception {
        if (exchangeAddress.equals(this.address) || exchangeAddress.equals(EMPTY_ADDRESS)) {
            throw new Exception("illegal exchange addr");
        }
        BaseContract baseContract = this.iContractsHelper.getContract(exchangeAddress);
        if (ObjectUtil.isNull(baseContract)) {
            throw new Exception(String.format("Get no %s contract instance", exchangeAddress));
        }
        if (baseContract instanceof SwapV1) {
            SwapV1 swapV1 = (SwapV1) baseContract;

            BigInteger trxBought = swapV1.trxToTokenOutput(tokensBought, minTrxSold);
            BigInteger tokenSold = trxToTokenOutput(trxBought, maxTokenSold);
            return tokenSold;
        } else {
            throw new Exception(String.format("Get %s contract instance not SwapV1"));
        }
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
