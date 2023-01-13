package org.tron.sunio.contract_mirror.mirror.contracts.impl;

import cn.hutool.core.util.ObjectUtil;
import lombok.extern.slf4j.Slf4j;
import org.tron.sunio.contract_mirror.event_decode.logdata.ContractEventLog;
import org.tron.sunio.contract_mirror.mirror.chainHelper.IChainHelper;
import org.tron.sunio.contract_mirror.mirror.contracts.BaseContract;
import org.tron.sunio.contract_mirror.mirror.dao.SwapV1Data;
import org.tron.sunio.contract_mirror.mirror.dao.SwapV2PairData;
import org.tron.sunio.contract_mirror.mirror.db.IDbHandler;
import org.tron.sunio.contract_mirror.mirror.enums.ContractType;

import java.util.Map;

import static org.tron.sunio.contract_mirror.event_decode.events.SwapV2PairEvent.EVENT_NAME_NEW_BURN;
import static org.tron.sunio.contract_mirror.event_decode.events.SwapV2PairEvent.EVENT_NAME_NEW_MINT;
import static org.tron.sunio.contract_mirror.event_decode.events.SwapV2PairEvent.EVENT_NAME_NEW_SWAP;
import static org.tron.sunio.contract_mirror.event_decode.events.SwapV2PairEvent.EVENT_NAME_NEW_SYNC;

@Slf4j
public class SwapV2Pair extends BaseContract {
    private String factory;

    public SwapV2Pair(String address, ContractType type, String factory, IChainHelper iChainHelper,
                      IDbHandler iDbHandler, Map<String, String> sigMap) {
        super(address, type, iChainHelper, iDbHandler, sigMap);
        this.factory = factory;
    }


    private void callChainData(SwapV2PairData swapV2PairData) {
        try {

        } catch (Exception e) {
        }
    }


    @Override
    public boolean initDataFromChain1() {
        SwapV2PairData swapV2PairData = iDbHandler.querySwapV2PairData(address);
        if (ObjectUtil.isNotNull(swapV2PairData)) {
            swapV2PairData = new SwapV2PairData();
            swapV2PairData.setFactory(factory);
            swapV2PairData.setType(type);
            swapV2PairData.setAddress(address);
            swapV2PairData.setUsing(true);
        }
        callChainData(swapV2PairData);
        iDbHandler.updateSwapV2PairData(swapV2PairData);
        return false;
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
            case EVENT_NAME_NEW_MINT:
                handleMint(topics, contractEventLog.getData());
                break;
            case EVENT_NAME_NEW_BURN:
                handleBurn(topics, contractEventLog.getData());
                break;
            case EVENT_NAME_NEW_SWAP:
                handleSwap(topics, contractEventLog.getData());
                break;
            case EVENT_NAME_NEW_SYNC:
                handleSync(topics, contractEventLog.getData());
                break;
            default:
                log.warn("event:{} not handle", topics[0]);
                break;
        }
    }

    private void handleMint(String[] topics, String data) {
        log.info("handleMint not implements!");
    }

    private void handleBurn(String[] topics, String data) {
        log.info("handleBurn not implements!");
    }

    private void handleSwap(String[] topics, String data) {
        log.info("handleSwap not implements!");
    }

    private void handleSync(String[] topics, String data) {
        log.info("handleSync not implements!");
    }
}
