package org.tron.sunio.contract_mirror.mirror.contracts.impl;

import cn.hutool.core.util.ObjectUtil;
import lombok.extern.slf4j.Slf4j;
import org.tron.sunio.contract_mirror.mirror.chainHelper.IChainHelper;
import org.tron.sunio.contract_mirror.mirror.chainHelper.TriggerContractInfo;
import org.tron.sunio.contract_mirror.mirror.consts.ContractMirrorConst;
import org.tron.sunio.contract_mirror.mirror.contracts.BaseContract;
import org.tron.sunio.contract_mirror.mirror.dao.SwapV2PairData;
import org.tron.sunio.contract_mirror.mirror.enums.ContractType;
import org.tron.sunio.tronsdk.WalletUtil;
import org.web3j.abi.EventValues;
import org.web3j.abi.TypeReference;
import org.web3j.abi.datatypes.Type;
import org.web3j.abi.datatypes.generated.Uint112;
import org.web3j.abi.datatypes.generated.Uint32;

import java.math.BigInteger;
import java.util.ArrayList;
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
    private static final BigInteger Q112 = BigInteger.TWO.pow(112);
    private String factory;
    private SwapV2PairData swapV2PairData;

    public SwapV2Pair(String address, String factory, IChainHelper iChainHelper,
                      Map<String, String> sigMap) {
        super(address, ContractType.SWAP_V2_PAIR, iChainHelper, sigMap);
        this.factory = factory;
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
            swapV2PairData.setReverse0((BigInteger) results.get(0).getValue());
            swapV2PairData.setReverse1((BigInteger) results.get(1).getValue());
            swapV2PairData.setBlockTimestampLast((long) results.get(2).getValue());
        } catch (Exception e) {
            log.error("SwapV2Pair :{} fail to get getReserves, size:{}", address, e.toString());
        }
    }

    @Override
    public boolean initDataFromChain1() {
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
    }

    @Override
    protected HandleResult handleEvent1(String eventName, String[] topics, String data, HandleEventExtraData handleEventExtraData) {
        HandleResult result;
        switch (eventName) {
            case EVENT_NAME_TRANSFER:
                result = handleTransfer(topics, data, handleEventExtraData);
                break;
            case EVENT_NAME_NEW_MINT:
                result = handleMint(topics, data);
                break;
            case EVENT_NAME_NEW_BURN:
                result = handleBurn(topics, data);
                break;
            case EVENT_NAME_NEW_SWAP:
                result = handleSwap(topics, data);
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
    public <T> T handleSpecialRequest(String method) {
        return null;
    }

    private HandleResult handleTransfer(String[] topics, String data, HandleEventExtraData handleEventExtraData) {
        EventValues eventValues = getEventValue(EVENT_NAME_TRANSFER, EVENT_NAME_TRANSFER_BODY, topics, data,
                handleEventExtraData.getUniqueId());
        if (ObjectUtil.isNull(eventValues)) {
            return HandleResult.genHandleFailMessage(String.format("Contract%s, type:%s decode handleTransfer fail!, unique id :%s",
                    address, type, handleEventExtraData.getUniqueId()));
        }
        SwapV2PairData swapV2PairData = this.getVarSwapV2PairData();
        String from = (String) eventValues.getIndexedValues().get(0).getValue();
        String to = (String) eventValues.getIndexedValues().get(0).getValue();
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

    private HandleResult handleMint(String[] topics, String data) {
        log.info("handleMint not implements!");
        return HandleResult.genHandleFailMessage("handleMint not implements!");
    }

    private HandleResult handleBurn(String[] topics, String data) {
        log.info("handleBurn not implements!");
        return HandleResult.genHandleFailMessage("handleBurn not implements!");
    }

    private HandleResult handleSwap(String[] topics, String data) {
        log.info("handleSwap not implements!");
        return HandleResult.genHandleFailMessage("handleSwap not implements!");
    }

    private HandleResult handleSync(String[] topics, String data, HandleEventExtraData handleEventExtraData) {
        EventValues eventValues = getEventValue(EVENT_NAME_NEW_SYNC, EVENT_NAME_NEW_SYNC_BODY, topics, data,
                handleEventExtraData.getUniqueId());
        if (ObjectUtil.isNull(eventValues)) {
            return HandleResult.genHandleFailMessage(String.format("Contract%s, type:%s decode handleSync fail!, unique id :%s",
                    address, type, handleEventExtraData.getUniqueId()));
        }
        SwapV2PairData swapV2PairData = this.getVarSwapV2PairData();
        BigInteger reserve0 = (BigInteger) eventValues.getNonIndexedValues().get(0).getValue();
        BigInteger reserve1 = (BigInteger) eventValues.getNonIndexedValues().get(1).getValue();
        //4294967296L = 2**32
        long blockTimestampLast = handleEventExtraData.getTimeStamp() % 4294967296L;
        long timeElapsed = blockTimestampLast - handleEventExtraData.getTimeStamp();
        BigInteger reserve0Origin = swapV2PairData.getReverse0();
        BigInteger reserve1Origin = swapV2PairData.getReverse1();
        if (timeElapsed > 0 && reserve0Origin.compareTo(BigInteger.ZERO) != 0 && reserve1Origin.compareTo(BigInteger.ZERO) != 0) {
            long price0Add = priceCumulativeLastAdd(reserve0Origin, reserve1Origin, timeElapsed);
            long price1Add = priceCumulativeLastAdd(reserve1Origin, reserve0Origin, timeElapsed);
            swapV2PairData.setPrice0CumulativeLast(swapV2PairData.getPrice0CumulativeLast() + price0Add);
            swapV2PairData.setPrice1CumulativeLast(swapV2PairData.getPrice1CumulativeLast() + price1Add);
        }
        swapV2PairData.setReverse0(reserve0);
        swapV2PairData.setReverse1(reserve1);
        swapV2PairData.setBlockTimestampLast(blockTimestampLast);
        isDirty = true;
        return HandleResult.genHandleSuccess();
    }

    private long priceCumulativeLastAdd(BigInteger reserve0, BigInteger reserve1, long timeElapsed) {
        return reserve1.multiply(Q112).divide(reserve0).longValue() * timeElapsed;
    }

}
