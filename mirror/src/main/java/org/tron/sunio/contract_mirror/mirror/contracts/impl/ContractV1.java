package org.tron.sunio.contract_mirror.mirror.contracts.impl;

import cn.hutool.core.util.ObjectUtil;
import lombok.extern.slf4j.Slf4j;
import org.tron.sunio.contract_mirror.event_decode.events.EventUtils;
import org.tron.sunio.contract_mirror.event_decode.logdata.ContractEventLog;
import org.tron.sunio.contract_mirror.mirror.cache.CacheHandler;
import org.tron.sunio.contract_mirror.mirror.chainHelper.IChainHelper;
import org.tron.sunio.contract_mirror.mirror.consts.ContractMirrorConst;
import org.tron.sunio.contract_mirror.mirror.contracts.BaseContract;
import org.tron.sunio.contract_mirror.mirror.dao.ContractV1Data;
import org.tron.sunio.contract_mirror.mirror.enums.ContractType;
import org.web3j.abi.EventValues;


import java.math.BigInteger;
import java.util.Arrays;
import java.util.Map;

import static org.tron.sunio.contract_mirror.event_decode.events.SwapV1Event.EVENT_NAME_ADD_LIQUIDITY;
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
public class ContractV1 extends BaseContract {

    private String tokenAddress;

    public ContractV1(String address, IChainHelper iChainHelper, String tokenAddress, final Map<String, String> sigMap) {
        super(address, ContractType.CONTRACT_V1, iChainHelper, sigMap);
        this.tokenAddress = tokenAddress;
    }

    @Override
    public boolean initDataFromChain1() {
        ContractV1Data v1Data = CacheHandler.v1Cache.getIfPresent(address);
        if (ObjectUtil.isNull(v1Data)) {
            v1Data = new ContractV1Data();
            v1Data.setType(this.type);
            v1Data.setAddress(this.address);
            v1Data.setTokenAddress(this.tokenAddress);
            v1Data.setUsing(true);
        }
        String name = callContractString(ContractMirrorConst.EMPTY_ADDRESS, "name");
        String symbol = callContractString(ContractMirrorConst.EMPTY_ADDRESS, "symbol");
        long decimals = callContractU256(ContractMirrorConst.EMPTY_ADDRESS, "decimals").longValue();
        int kLast = 0;
        try {
            kLast = callContractUint(ContractMirrorConst.EMPTY_ADDRESS, "kLast").intValue();
        } catch (Exception e) {
            log.error("Contract swap v1:{}  get kLast failed", address);
        }
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
        CacheHandler.v1Cache.put(this.address, v1Data);
        return true;
    }

    @Override
    public void updateBaseInfoToCache(boolean isUsing, boolean isReady, boolean isAddExchangeContracts) {
        ContractV1Data v1Data = CacheHandler.v1Cache.getIfPresent(address);
        v1Data.setReady(isReady);
        v1Data.setUsing(isUsing);
        v1Data.setAddExchangeContracts(isAddExchangeContracts);
        CacheHandler.v1Cache.put(this.address, v1Data);
    }

    @Override
    public void handleEvent(ContractEventLog contractEventLog) {
        super.handleEvent(contractEventLog);
        if (!isReady) {
            return;
        }
        // Do handleEvent
        String eventName = getEventName(contractEventLog);
        String[] topics = contractEventLog.getTopicList();
        switch (eventName) {
            case EVENT_NAME_TRANSFER:
                handleEventTransfer(topics, contractEventLog.getData());
                break;
            case EVENT_NAME_TOKEN_PURCHASE:
                handleTokenPurchase(topics, contractEventLog.getData());
                break;
            case EVENT_NAME_TRX_PURCHASE:
                handleTrxPurchase(topics, contractEventLog.getData());
                break;
            case EVENT_NAME_TOKEN_TO_TOKEN:
                handleTokenToToken(topics, contractEventLog.getData());
                break;
            case EVENT_NAME_SNAPSHOT:
                handleEventSnapshot(topics, contractEventLog.getData());
                break;
            case EVENT_NAME_ADD_LIQUIDITY:
                handleAddLiquidity(topics, contractEventLog.getData());
                break;
            case EVENT_NAME_REMOVE_LIQUIDITY:
                handleRemoveLiquidity(topics, contractEventLog.getData());
                break;
            default:
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
        ContractV1Data v1Data = CacheHandler.v1Cache.getIfPresent(tokenAddress);
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
            CacheHandler.v1Cache.put(this.address, v1Data);
        }
    }

    private void handleEventSnapshot(String[] topics, String data) {
        EventValues values = EventUtils.getEventValue(EVENT_NAME_SNAPSHOT_BODY,
                Arrays.asList(topics), data, false);
        if (ObjectUtil.isNull(values)) {
            log.error("handEventFeeRate failed!!");
            return;
        }
        ContractV1Data v1Data = CacheHandler.v1Cache.getIfPresent(tokenAddress);
        BigInteger trx = (BigInteger) values.getIndexedValues().get(1).getValue();
        BigInteger tokenBalance = (BigInteger) values.getIndexedValues().get(2).getValue();
        v1Data.setTokenBalance(tokenBalance);
        v1Data.setTrxBalance(trx);
        CacheHandler.v1Cache.put(this.address, v1Data);
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
