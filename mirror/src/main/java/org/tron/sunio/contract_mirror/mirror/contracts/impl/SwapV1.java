package org.tron.sunio.contract_mirror.mirror.contracts.impl;

import cn.hutool.core.util.ObjectUtil;
import lombok.extern.slf4j.Slf4j;
import org.tron.sunio.contract_mirror.event_decode.events.EventUtils;
import org.tron.sunio.contract_mirror.mirror.db.IDbHandler;
import org.tron.sunio.contract_mirror.mirror.chainHelper.IChainHelper;
import org.tron.sunio.contract_mirror.mirror.consts.ContractMirrorConst;
import org.tron.sunio.contract_mirror.mirror.contracts.BaseContract;
import org.tron.sunio.contract_mirror.mirror.dao.SwapV1Data;
import org.tron.sunio.contract_mirror.mirror.enums.ContractType;
import org.web3j.abi.EventValues;


import java.math.BigInteger;
import java.util.Arrays;
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
import static org.tron.sunio.contract_mirror.mirror.consts.ContractMirrorConst.EMPTY_TOPIC_VALUE;

@Slf4j
public class SwapV1 extends BaseContract {

    private String tokenAddress;
    private SwapV1Data swapV1Data;

    public SwapV1(String address, IChainHelper iChainHelper, IDbHandler iDbHandler, String tokenAddress,
                  final Map<String, String> sigMap) {
        super(address, ContractType.SWAP_V1, iChainHelper, iDbHandler, sigMap);
        this.tokenAddress = tokenAddress;
    }

    private SwapV1Data getVarSwapV1Data() {
        if (ObjectUtil.isNull(swapV1Data)) {
            swapV1Data = iDbHandler.querySwapV1Data(address);
            if (ObjectUtil.isNull(swapV1Data)) {
                swapV1Data = new SwapV1Data();
                swapV1Data.setType(this.type);
                swapV1Data.setAddress(this.address);
                swapV1Data.setTokenAddress(this.tokenAddress);
                swapV1Data.setUsing(true);
            }
        }
        return swapV1Data;
    }

    @Override
    public boolean initDataFromChain1() {
        try {
            SwapV1Data v1Data = this.getVarSwapV1Data();
            String name = callContractString(ContractMirrorConst.EMPTY_ADDRESS, "name");
            String symbol = callContractString(ContractMirrorConst.EMPTY_ADDRESS, "symbol");
            long decimals = callContractU256(ContractMirrorConst.EMPTY_ADDRESS, "decimals").longValue();
            long kLast = callContractUint(ContractMirrorConst.EMPTY_ADDRESS, "kLast");
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
            v1Data.setReady(isReady);
            isDirty = true;
            return true;
        } catch (Exception e) {
            log.error("Contract:{} type:{}, failed at function CallChainData:{}", address, type, e.toString());
            return false;
        }
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
        SwapV1Data v1Data = this.getVarSwapV1Data();
        iDbHandler.updateSwapV1Data(v1Data);
    }

    @Override
    protected void handleEvent1(String eventName, String[] topics, String data, HandleEventExtraData handleEventExtraData) {
        switch (eventName) {
            case EVENT_NAME_TRANSFER:
                handleEventTransfer(topics, data);
                break;
            case EVENT_NAME_TOKEN_PURCHASE:
                handleTokenPurchase(topics, data);
                break;
            case EVENT_NAME_TRX_PURCHASE:
                handleTrxPurchase(topics, data);
                break;
            case EVENT_NAME_TOKEN_TO_TOKEN:
                handleTokenToToken(topics, data);
                break;
            case EVENT_NAME_SNAPSHOT:
                handleEventSnapshot(topics, data);
                break;
            case EVENT_NAME_ADD_LIQUIDITY:
                handleAddLiquidity(topics, data);
                break;
            case EVENT_NAME_REMOVE_LIQUIDITY:
                handleRemoveLiquidity(topics, data);
                break;
            case EVENT_NAME_ADMIN_FEE_MINT:
                handleAdminFeeMint(topics, data);
                break;
            default:
                log.warn("Contract:{} type:{} event:{} not handle", address, type, topics[0]);
                break;
        }
    }

    private void handleEventTransfer(String[] topics, String data) {
        EventValues values = EventUtils.getEventValue(EVENT_NAME_TRANSFER_BODY,
                Arrays.asList(topics), data, false);
        if (ObjectUtil.isNull(values)) {
            log.error("handEventFeeRate failed!!");
            return;
        }
        SwapV1Data v1Data = this.getVarSwapV1Data();
        String from = (String) values.getIndexedValues().get(0).getValue();
        String to = (String) values.getIndexedValues().get(0).getValue();
        BigInteger amount = (BigInteger) values.getNonIndexedValues().get(0).getValue();
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
    }

    private void handleEventSnapshot(String[] topics, String data) {
        EventValues values = EventUtils.getEventValue(EVENT_NAME_SNAPSHOT_BODY,
                Arrays.asList(topics), data, false);
        if (ObjectUtil.isNull(values)) {
            log.error("handEventFeeRate failed!!");
            return;
        }
        SwapV1Data v1Data = this.getVarSwapV1Data();
        BigInteger trx = (BigInteger) values.getIndexedValues().get(1).getValue();
        BigInteger tokenBalance = (BigInteger) values.getIndexedValues().get(2).getValue();
        v1Data.setTokenBalance(tokenBalance);
        v1Data.setTrxBalance(trx);
        isDirty = true;
    }

    private void handleAddLiquidity(String[] topics, String data) {
        log.info("handleAddLiquidity not implements!");
    }

    private void handleRemoveLiquidity(String[] topics, String data) {
        log.info("handleRemoveLiquidity not implements!");
    }

    private void handleAdminFeeMint(String[] topics, String data) {
        log.info("handleAdminFeeMint not implements!");
    }

    private void handleTokenPurchase(String[] topics, String data) {
        log.info("handleTokenPurchase not implements!");
    }

    private void handleTrxPurchase(String[] topics, String data) {
        log.info("handleTrxPurchase not implements!");
    }

    private void handleTokenToToken(String[] topics, String data) {
        log.info("TokenToToken not implements!");
    }
}
