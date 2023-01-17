package org.tron.sunio.contract_mirror.mirror.contracts.impl;

import cn.hutool.core.util.ObjectUtil;
import lombok.extern.slf4j.Slf4j;
import org.tron.sunio.contract_mirror.event_decode.events.EventUtils;
import org.tron.sunio.contract_mirror.mirror.chainHelper.IChainHelper;
import org.tron.sunio.contract_mirror.mirror.chainHelper.TriggerContractInfo;
import org.tron.sunio.contract_mirror.mirror.consts.ContractMirrorConst;
import org.tron.sunio.contract_mirror.mirror.contracts.BaseContract;
import org.tron.sunio.contract_mirror.mirror.contracts.events.IContractEventWrap;
import org.tron.sunio.contract_mirror.mirror.dao.SwapV2PairData;
import org.tron.sunio.contract_mirror.mirror.db.IDbHandler;
import org.tron.sunio.contract_mirror.mirror.enums.ContractType;
import org.tron.sunio.tronsdk.WalletUtil;
import org.web3j.abi.EventValues;
import org.web3j.abi.TypeReference;
import org.web3j.abi.datatypes.Type;
import org.web3j.abi.datatypes.generated.Uint112;
import org.web3j.abi.datatypes.generated.Uint32;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.tron.sunio.contract_mirror.event_decode.events.SwapV2PairEvent.EVENT_NAME_NEW_SYNC_BODY;
import static org.tron.sunio.contract_mirror.event_decode.events.SwapV2PairEvent.EVENT_NAME_TRANSFER_BODY;
import static org.tron.sunio.contract_mirror.event_decode.events.SwapV2PairEvent.EVENT_NAME_NEW_BURN;
import static org.tron.sunio.contract_mirror.event_decode.events.SwapV2PairEvent.EVENT_NAME_NEW_MINT;
import static org.tron.sunio.contract_mirror.event_decode.events.SwapV2PairEvent.EVENT_NAME_NEW_SWAP;
import static org.tron.sunio.contract_mirror.event_decode.events.SwapV2PairEvent.EVENT_NAME_NEW_SYNC;
import static org.tron.sunio.contract_mirror.event_decode.events.SwapV2PairEvent.EVENT_NAME_TRANSFER;
import static org.tron.sunio.contract_mirror.mirror.consts.ContractMirrorConst.EMPTY_TOPIC_VALUE;

@Slf4j
public class SwapV2Pair extends BaseContract {
    private String factory;
    private SwapV2PairData swapV2PairData;

    public SwapV2Pair(String address, String factory, IChainHelper iChainHelper,
                      IDbHandler iDbHandler, Map<String, String> sigMap) {
        super(address, ContractType.SWAP_V2_PAIR, iChainHelper, iDbHandler, sigMap);
        this.factory = factory;
    }

    private SwapV2PairData getVarSwapV2PairData() {
        if (ObjectUtil.isNull(swapV2PairData)) {
            swapV2PairData = iDbHandler.querySwapV2PairData(address);
            if (ObjectUtil.isNotNull(swapV2PairData)) {
                swapV2PairData = new SwapV2PairData();
                swapV2PairData.setFactory(factory);
                swapV2PairData.setType(type);
                swapV2PairData.setAddress(address);
                swapV2PairData.setUsing(true);
            }
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
            swapV2PairData.setReverse0((BigInteger) results.get(0).getValue());
            swapV2PairData.setReverse1((BigInteger) results.get(1).getValue());
            swapV2PairData.setBlockTimestampLast((long) results.get(2).getValue());
        } catch (Exception e) {
            log.error("SwapV2Pair :{} fail to get getReserves, size:{}", address, e.toString());
        }
    }

    @Override
    public boolean initDataFromChain1() {
        try {
            SwapV2PairData swapV2PairData = this.getVarSwapV2PairData();
            String token0 = WalletUtil.ethAddressToTron(callContractAddress(ContractMirrorConst.EMPTY_ADDRESS, "token0").toString());
            swapV2PairData.setToken0(token0);
            String token1 = WalletUtil.ethAddressToTron(callContractAddress(ContractMirrorConst.EMPTY_ADDRESS, "token1").toString());
            swapV2PairData.setToken1(token1);
            callReservesOnChain(swapV2PairData);
            swapV2PairData.setPrice0CumulativeLast(callContractUint(ContractMirrorConst.EMPTY_ADDRESS, "price0CumulativeLast()"));
            swapV2PairData.setPrice1CumulativeLast(callContractUint(ContractMirrorConst.EMPTY_ADDRESS, "price1CumulativeLast()"));
            String name = callContractString(ContractMirrorConst.EMPTY_ADDRESS, "name");
            swapV2PairData.setName(name);
            String symbol = callContractString(ContractMirrorConst.EMPTY_ADDRESS, "symbol");
            swapV2PairData.setSymbol(symbol);
            long decimals = callContractU256(ContractMirrorConst.EMPTY_ADDRESS, "decimals").longValue();
            swapV2PairData.setDecimals(decimals);
            BigInteger kLast = callContractU256(ContractMirrorConst.EMPTY_ADDRESS, "kLast");
            swapV2PairData.setKLast(kLast);
            BigInteger lpTotalSupply = callContractU256(ContractMirrorConst.EMPTY_ADDRESS, "totalSupply");
            swapV2PairData.setLpTotalSupply(lpTotalSupply);
            BigInteger trxBalance = getBalance(address);
            swapV2PairData.setTrxBalance(trxBalance);
            isDirty = true;
            return true;
        } catch (Exception e) {
            return false;
        }
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
    protected void saveUpdateToCache() {
        SwapV2PairData swapV2PairData = this.getVarSwapV2PairData();
        iDbHandler.updateSwapV2PairData(swapV2PairData);
    }

    @Override
    public void handleEvent(IContractEventWrap iContractEventWrap) {
        super.handleEvent(iContractEventWrap);
        if (!isReady) {
            return;
        }
        // Do handleEvent
        String eventName = getEventName(iContractEventWrap);
        String[] topics = iContractEventWrap.getTopics();
        switch (eventName) {
            case EVENT_NAME_TRANSFER:
                handleTransfer(topics, iContractEventWrap.getData());
                break;
            case EVENT_NAME_NEW_MINT:
                handleMint(topics, iContractEventWrap.getData());
                break;
            case EVENT_NAME_NEW_BURN:
                handleBurn(topics, iContractEventWrap.getData());
                break;
            case EVENT_NAME_NEW_SWAP:
                handleSwap(topics, iContractEventWrap.getData());
                break;
            case EVENT_NAME_NEW_SYNC:
                handleSync(topics, iContractEventWrap.getData());
                break;
            default:
                log.warn("event:{} not handle", topics[0]);
                break;
        }
    }

    private void handleTransfer(String[] topics, String data) {
        EventValues values = EventUtils.getEventValue(EVENT_NAME_TRANSFER_BODY,
                Arrays.asList(topics), data, false);
        if (ObjectUtil.isNull(values)) {
            log.error("handEventFeeRate failed!!");
            return;
        }
        SwapV2PairData swapV2PairData = this.getVarSwapV2PairData();
        String from = (String) values.getIndexedValues().get(0).getValue();
        String to = (String) values.getIndexedValues().get(0).getValue();
        BigInteger amount = (BigInteger) values.getNonIndexedValues().get(0).getValue();
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
        EventValues values = EventUtils.getEventValue(EVENT_NAME_NEW_SYNC_BODY,
                Arrays.asList(topics), data, false);
        if (ObjectUtil.isNull(values)) {
            log.error("handleSync failed!!");
            return;
        }
        SwapV2PairData swapV2PairData = this.getVarSwapV2PairData();
        BigInteger reserve0 = (BigInteger) values.getNonIndexedValues().get(0).getValue();
        BigInteger reserve1 = (BigInteger) values.getNonIndexedValues().get(1).getValue();
        swapV2PairData.setReverse0(reserve0);
        swapV2PairData.setReverse1(reserve1);
        isDirty = true;
    }
}
