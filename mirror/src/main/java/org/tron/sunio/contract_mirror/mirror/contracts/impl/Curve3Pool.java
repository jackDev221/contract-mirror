package org.tron.sunio.contract_mirror.mirror.contracts.impl;

import cn.hutool.core.util.ObjectUtil;
import lombok.extern.slf4j.Slf4j;
import org.tron.sunio.contract_mirror.mirror.chainHelper.IChainHelper;
import org.tron.sunio.contract_mirror.mirror.consts.ContractMirrorConst;
import org.tron.sunio.contract_mirror.mirror.contracts.BaseContract;
import org.tron.sunio.contract_mirror.mirror.contracts.events.IContractEventWrap;
import org.tron.sunio.contract_mirror.mirror.dao.Curve3PoolData;
import org.tron.sunio.contract_mirror.mirror.db.IDbHandler;
import org.tron.sunio.contract_mirror.mirror.enums.ContractType;
import org.tron.sunio.tronsdk.WalletUtil;

import java.math.BigInteger;
import java.util.Map;

import static org.tron.sunio.contract_mirror.event_decode.events.Curve3PoolEvent.EVENT_NAME_ADD_LIQUIDITY;
import static org.tron.sunio.contract_mirror.event_decode.events.Curve3PoolEvent.EVENT_NAME_COMMIT_NEW_ADMIN;
import static org.tron.sunio.contract_mirror.event_decode.events.Curve3PoolEvent.EVENT_NAME_COMMIT_NEW_FEE;
import static org.tron.sunio.contract_mirror.event_decode.events.Curve3PoolEvent.EVENT_NAME_NEW_ADMIN;
import static org.tron.sunio.contract_mirror.event_decode.events.Curve3PoolEvent.EVENT_NAME_NEW_FEE;
import static org.tron.sunio.contract_mirror.event_decode.events.Curve3PoolEvent.EVENT_NAME_NEW_FEE_CONVERTER;
import static org.tron.sunio.contract_mirror.event_decode.events.Curve3PoolEvent.EVENT_NAME_RAMP_A;
import static org.tron.sunio.contract_mirror.event_decode.events.Curve3PoolEvent.EVENT_NAME_REMOVE_LIQUIDITY;
import static org.tron.sunio.contract_mirror.event_decode.events.Curve3PoolEvent.EVENT_NAME_REMOVE_LIQUIDITY_IM_BALANCE;
import static org.tron.sunio.contract_mirror.event_decode.events.Curve3PoolEvent.EVENT_NAME_REMOVE_LIQUIDITY_ONE;
import static org.tron.sunio.contract_mirror.event_decode.events.Curve3PoolEvent.EVENT_NAME_STOP_RAMP_A;
import static org.tron.sunio.contract_mirror.event_decode.events.Curve3PoolEvent.EVENT_NAME_TOKEN_EXCHANGE;
import static org.tron.sunio.contract_mirror.event_decode.events.Curve3PoolEvent.EVENT_NAME_TRANSFER;

@Slf4j
public class Curve3Pool extends BaseContract {

    public Curve3Pool(String address, IChainHelper iChainHelper, IDbHandler iDbHandler, Map<String, String> sigMap) {
        super(address, ContractType.CONTRACT_CURVE_3POOL, iChainHelper, iDbHandler, sigMap);
    }

    @Override
    public boolean initDataFromChain1() {
        Curve3PoolData curve3PoolData = iDbHandler.queryCurve3PoolData(address);
        if (ObjectUtil.isNull(curve3PoolData)) {
            curve3PoolData = new Curve3PoolData();
            curve3PoolData.setAddress(address);
            curve3PoolData.setType(type);
            curve3PoolData.setUsing(true);
            curve3PoolData.setReady(false);
            curve3PoolData.setAddExchangeContracts(false);
        }
        callChainData(curve3PoolData);
        iDbHandler.updateCurve3PoolData(curve3PoolData);
        return true;
    }

    private void callChainData(Curve3PoolData curve3PoolData) {
        try {


            String token = WalletUtil.ethAddressToTron(callContractAddress(ContractMirrorConst.EMPTY_ADDRESS, "token").toString());
            curve3PoolData.setToken(token);
            BigInteger fee = callContractU256(ContractMirrorConst.EMPTY_ADDRESS, "fee");
            curve3PoolData.setFee(fee);
            BigInteger futureFee = callContractU256(ContractMirrorConst.EMPTY_ADDRESS, "future_fee");
            curve3PoolData.setFutureFee(futureFee);
            BigInteger adminFee = callContractU256(ContractMirrorConst.EMPTY_ADDRESS, "admin_fee");
            curve3PoolData.setAdminFee(adminFee);
            BigInteger futureAdminFee = callContractU256(ContractMirrorConst.EMPTY_ADDRESS, "future_admin_fee");
            curve3PoolData.setFutureAdminFee(futureAdminFee);
            BigInteger adminActionsDeadline = callContractU256(ContractMirrorConst.EMPTY_ADDRESS, "admin_actions_deadline");
            curve3PoolData.setAdminActionsDeadline(adminActionsDeadline);
            String feeConverter = WalletUtil.ethAddressToTron(callContractAddress(ContractMirrorConst.EMPTY_ADDRESS, "fee_converter").toString());
            curve3PoolData.setFeeConverter(feeConverter);
            BigInteger initialA = callContractU256(ContractMirrorConst.EMPTY_ADDRESS, "initial_A");
            curve3PoolData.setInitialA(initialA);
            BigInteger initialATime = callContractU256(ContractMirrorConst.EMPTY_ADDRESS, "initial_A_time");
            curve3PoolData.setInitialATime(initialATime);
            BigInteger futureA = callContractU256(ContractMirrorConst.EMPTY_ADDRESS, "future_A");
            curve3PoolData.setFutureA(futureA);
            BigInteger futureATime = callContractU256(ContractMirrorConst.EMPTY_ADDRESS, "future_A_time");
            curve3PoolData.setFutureATime(futureATime);
            String owner = WalletUtil.ethAddressToTron(callContractAddress(ContractMirrorConst.EMPTY_ADDRESS, "owner").toString());
            curve3PoolData.setOwner(owner);
            String futureOwner = WalletUtil.ethAddressToTron(callContractAddress(ContractMirrorConst.EMPTY_ADDRESS, "future_owner").toString());
            curve3PoolData.setFutureOwner(futureOwner);
            BigInteger transferOwnershipDeadline = callContractU256(ContractMirrorConst.EMPTY_ADDRESS, "transfer_ownership_deadline");
            curve3PoolData.setTransferOwnershipDeadline(transferOwnershipDeadline);
        } catch (Exception e) {
            log.error("Contract:{} type:{}, failed at function CallChainData:{}", address, type, e.toString());
        }
    }

    @Override
    public void updateBaseInfoToCache(boolean isUsing, boolean isReady, boolean isAddExchangeContracts) {
        Curve3PoolData curve3PoolData = iDbHandler.queryCurve3PoolData(address);
        curve3PoolData.setUsing(isUsing);
        curve3PoolData.setReady(isReady);
        curve3PoolData.setAddExchangeContracts(isAddExchangeContracts);
        iDbHandler.updateCurve3PoolData(curve3PoolData);
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
                handleEventTransfer(topics, iContractEventWrap.getData());
                break;
            case EVENT_NAME_TOKEN_EXCHANGE:
                handleEventTokenExchange(topics, iContractEventWrap.getData());
                break;
            case EVENT_NAME_ADD_LIQUIDITY:
                handleEventAddLiquidity(topics, iContractEventWrap.getData());
                break;
            case EVENT_NAME_REMOVE_LIQUIDITY:
                handleEventRemoveLiquidity(topics, iContractEventWrap.getData());
                break;
            case EVENT_NAME_REMOVE_LIQUIDITY_ONE:
                handleEventRemoveLiquidityOne(topics, iContractEventWrap.getData());
                break;
            case EVENT_NAME_REMOVE_LIQUIDITY_IM_BALANCE:
                handleEventRemoveLiquidityImbalance(topics, iContractEventWrap.getData());
                break;
            case EVENT_NAME_COMMIT_NEW_ADMIN:
                handleEventCommitNewAdmin(topics, iContractEventWrap.getData());
                break;
            case EVENT_NAME_NEW_ADMIN:
                handleEventNewAdmin(topics, iContractEventWrap.getData());
                break;
            case EVENT_NAME_NEW_FEE_CONVERTER:
                handleEventNewFeeConverter(topics, iContractEventWrap.getData());
                break;
            case EVENT_NAME_COMMIT_NEW_FEE:
                handleEventCommitNewFee(topics, iContractEventWrap.getData());
                break;
            case EVENT_NAME_NEW_FEE:
                handleEventNewFee(topics, iContractEventWrap.getData());
                break;
            case EVENT_NAME_RAMP_A:
                handleEventRampA(topics, iContractEventWrap.getData());
                break;
            case EVENT_NAME_STOP_RAMP_A:
                handleEventStopRampA(topics, iContractEventWrap.getData());
                break;
        }

    }

    private void handleEventTransfer(String[] topics, String data) {
        log.info("handleEventTransfer not implements!");
    }

    private void handleEventTokenExchange(String[] topics, String data) {
        log.info("handleEventTokenExchange not implements!");
    }

    private void handleEventAddLiquidity(String[] topics, String data) {
        log.info("handleEventAddLiquidity not implements!");
    }

    private void handleEventRemoveLiquidity(String[] topics, String data) {
        log.info("handleEventRemoveLiquidity not implements!");
    }

    private void handleEventRemoveLiquidityOne(String[] topics, String data) {
        this.isReady = false;
        updateBaseInfoToCache(isUsing, false, isAddExchangeContracts);
    }

    private void handleEventRemoveLiquidityImbalance(String[] topics, String data) {
        log.info("handleEventRemoveLiquidityImbalance not implements!");
    }

    private void handleEventCommitNewAdmin(String[] topics, String data) {
        log.info("handleEventCommitNewAdmin not implements!");
    }

    private void handleEventNewAdmin(String[] topics, String data) {
        log.info("handleEventNewAdmin not implements!");
    }

    private void handleEventNewFeeConverter(String[] topics, String data) {
        log.info("handleEventNewFeeConverter not implements!");
    }

    private void handleEventCommitNewFee(String[] topics, String data) {
        log.info("handleRemoveLiquidity not implements!");
    }

    private void handleEventNewFee(String[] topics, String data) {
        log.info("handleRemoveLiquidity not implements!");
    }

    private void handleEventRampA(String[] topics, String data) {
        log.info("handleRemoveLiquidity not implements!");
    }

    private void handleEventStopRampA(String[] topics, String data) {
        log.info("handleRemoveLiquidity not implements!");
    }
}
