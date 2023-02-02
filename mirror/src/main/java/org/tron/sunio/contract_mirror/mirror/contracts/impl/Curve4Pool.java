package org.tron.sunio.contract_mirror.mirror.contracts.impl;

import cn.hutool.core.util.ObjectUtil;
import lombok.extern.slf4j.Slf4j;
import org.tron.sunio.contract_mirror.mirror.chainHelper.IChainHelper;
import org.tron.sunio.contract_mirror.mirror.chainHelper.TriggerContractInfo;
import org.tron.sunio.contract_mirror.mirror.consts.ContractMirrorConst;
import org.tron.sunio.contract_mirror.mirror.contracts.BaseContract;
import org.tron.sunio.contract_mirror.mirror.dao.Curve4PoolData;
import org.tron.sunio.contract_mirror.mirror.enums.ContractType;
import org.tron.sunio.tronsdk.WalletUtil;
import org.web3j.abi.TypeReference;
import org.web3j.abi.datatypes.Address;
import org.web3j.abi.datatypes.Type;
import org.web3j.abi.datatypes.generated.Uint256;

import java.math.BigInteger;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.tron.sunio.contract_mirror.event_decode.events.Curve4PoolEvent.EVENT_NAME_ADD_LIQUIDITY;
import static org.tron.sunio.contract_mirror.event_decode.events.Curve4PoolEvent.EVENT_NAME_COMMIT_NEW_ADMIN;
import static org.tron.sunio.contract_mirror.event_decode.events.Curve4PoolEvent.EVENT_NAME_COMMIT_NEW_FEE;
import static org.tron.sunio.contract_mirror.event_decode.events.Curve4PoolEvent.EVENT_NAME_NEW_ADMIN;
import static org.tron.sunio.contract_mirror.event_decode.events.Curve4PoolEvent.EVENT_NAME_NEW_FEE;
import static org.tron.sunio.contract_mirror.event_decode.events.Curve4PoolEvent.EVENT_NAME_NEW_FEE_CONVERTER;
import static org.tron.sunio.contract_mirror.event_decode.events.Curve4PoolEvent.EVENT_NAME_RAMP_A;
import static org.tron.sunio.contract_mirror.event_decode.events.Curve4PoolEvent.EVENT_NAME_REMOVE_LIQUIDITY;
import static org.tron.sunio.contract_mirror.event_decode.events.Curve4PoolEvent.EVENT_NAME_REMOVE_LIQUIDITY_IM_BALANCE;
import static org.tron.sunio.contract_mirror.event_decode.events.Curve4PoolEvent.EVENT_NAME_REMOVE_LIQUIDITY_ONE;
import static org.tron.sunio.contract_mirror.event_decode.events.Curve4PoolEvent.EVENT_NAME_STOP_RAMP_A;
import static org.tron.sunio.contract_mirror.event_decode.events.Curve4PoolEvent.EVENT_NAME_TOKEN_EXCHANGE;
import static org.tron.sunio.contract_mirror.event_decode.events.Curve4PoolEvent.EVENT_NAME_TOKEN_EXCHANGE_UNDERLING;

@Slf4j
public class Curve4Pool extends BaseContract {
    private static final int N_COINS = 4;
    private static final BigInteger FEE_DENOMINATOR = BigInteger.TEN.pow(10);
    private Curve4PoolData curve4PoolData;

    public Curve4Pool(String address, IChainHelper iChainHelper, Map<String, String> sigMap) {
        super(address, ContractType.CONTRACT_CURVE_4POOL, iChainHelper, sigMap);
    }

    private Curve4PoolData getVarCurve4PoolData() {
        if (ObjectUtil.isNull(curve4PoolData)) {
            curve4PoolData = new Curve4PoolData();
            curve4PoolData.setAddress(address);
            curve4PoolData.setType(type);
            curve4PoolData.setUsing(true);
            curve4PoolData.setReady(false);
            curve4PoolData.setAddExchangeContracts(false);
        }
        return curve4PoolData;
    }

    private void updateCoinsAndBalance(Curve4PoolData curve4PoolData) {
        for (int i = 0; i < N_COINS; i++) {
            // update coins string
            TriggerContractInfo triggerContractInfo = new TriggerContractInfo(ContractMirrorConst.EMPTY_ADDRESS, address,
                    "coins", List.of(new Uint256(i)), List.of(new TypeReference<Address>() {
            })
            );
            List<Type> results = this.iChainHelper.triggerConstantContract(triggerContractInfo);
            if (results.size() == 0) {
                log.error("Get contract:{} type:{} , function:{} result len is zero", this.address, this.type, "coins");
            } else {
                curve4PoolData.updateCoins(i, WalletUtil.hexStringToTron((String) results.get(0).getValue()));
            }

            // update base_coins
            results = this.iChainHelper.triggerConstantContract(triggerContractInfo);
            if (results.size() == 0) {
                log.error("Get contract:{} type:{} , function:{} result len is zero", this.address, this.type, "base_coins");
            } else {
                curve4PoolData.updateBaseCoins(i, WalletUtil.hexStringToTron((String) results.get(0).getValue()));
            }

            // update coins balance
            triggerContractInfo = new TriggerContractInfo(ContractMirrorConst.EMPTY_ADDRESS, address, "balances",
                    List.of(new Uint256(i)), List.of(new TypeReference<Uint256>() {
            }));
            results = this.iChainHelper.triggerConstantContract(triggerContractInfo);
            if (results.size() == 0) {
                log.error("Get contract:{} type:{} , function:{} result len is zero", this.address, this.type, "balances");
            } else {
                curve4PoolData.updateBalances(i, (BigInteger) results.get(0).getValue());
            }
        }
    }

    private void updateSupply(Curve4PoolData curve4PoolData, String tokenAddress) {
        TriggerContractInfo triggerContractInfo = new TriggerContractInfo(ContractMirrorConst.EMPTY_ADDRESS, this.getAddress(),
                tokenAddress, Collections.emptyList(), List.of(new TypeReference<Uint256>() {
        }));
        List<Type> results = this.iChainHelper.triggerConstantContract(triggerContractInfo);
        if (results.size() == 0) {
            log.error("Get contract:{} type:{} , token: {} function: totalSupply() result len is zero", this.address, this.type, tokenAddress);
        } else {
            curve4PoolData.setTotalSupply((BigInteger) results.get(0).getValue());
        }
    }

    @Override
    public boolean initDataFromChain1() {
        Curve4PoolData curve4PoolData = this.getVarCurve4PoolData();
        updateCoinsAndBalance(curve4PoolData);
        String token = WalletUtil.ethAddressToTron(callContractAddress(ContractMirrorConst.EMPTY_ADDRESS, "lp_token").toString());
        curve4PoolData.setLpToken(token);
        //TODO check
        updateSupply(curve4PoolData, token);

        String basePool = WalletUtil.ethAddressToTron(callContractAddress(ContractMirrorConst.EMPTY_ADDRESS, "base_pool").toString());
        curve4PoolData.setBasePool(basePool);
        String baseLp = WalletUtil.ethAddressToTron(callContractAddress(ContractMirrorConst.EMPTY_ADDRESS, "base_lp").toString());
        curve4PoolData.setBaseLp(baseLp);
        BigInteger baseVirtualPrice = callContractU256(ContractMirrorConst.EMPTY_ADDRESS, "base_virtual_price");
        curve4PoolData.setBaseVirtualPrice(baseVirtualPrice);
        BigInteger baseCacheUpdated = callContractU256(ContractMirrorConst.EMPTY_ADDRESS, "base_cache_updated");
        curve4PoolData.setBaseCacheUpdated(baseCacheUpdated.longValue());
        BigInteger fee = callContractU256(ContractMirrorConst.EMPTY_ADDRESS, "fee");
        curve4PoolData.setFee(fee);
        BigInteger futureFee = callContractU256(ContractMirrorConst.EMPTY_ADDRESS, "future_fee");
        curve4PoolData.setFutureFee(futureFee);
        BigInteger adminFee = callContractU256(ContractMirrorConst.EMPTY_ADDRESS, "admin_fee");
        curve4PoolData.setAdminFee(adminFee);
        BigInteger futureAdminFee = callContractU256(ContractMirrorConst.EMPTY_ADDRESS, "future_admin_fee");
        curve4PoolData.setFutureAdminFee(futureAdminFee);
        BigInteger adminActionsDeadline = callContractU256(ContractMirrorConst.EMPTY_ADDRESS, "admin_actions_deadline");
        curve4PoolData.setAdminActionsDeadline(adminActionsDeadline);
        String feeConverter = WalletUtil.ethAddressToTron(callContractAddress(ContractMirrorConst.EMPTY_ADDRESS, "fee_converter").toString());
        curve4PoolData.setFeeConverter(feeConverter);
        BigInteger initialA = callContractU256(ContractMirrorConst.EMPTY_ADDRESS, "initial_A");
        curve4PoolData.setInitialA(initialA);
        BigInteger initialATime = callContractU256(ContractMirrorConst.EMPTY_ADDRESS, "initial_A_time");
        curve4PoolData.setInitialATime(initialATime);
        BigInteger futureA = callContractU256(ContractMirrorConst.EMPTY_ADDRESS, "future_A");
        curve4PoolData.setFutureA(futureA);
        BigInteger futureATime = callContractU256(ContractMirrorConst.EMPTY_ADDRESS, "future_A_time");
        curve4PoolData.setFutureATime(futureATime);
        String owner = WalletUtil.ethAddressToTron(callContractAddress(ContractMirrorConst.EMPTY_ADDRESS, "owner").toString());
        curve4PoolData.setOwner(owner);
        String futureOwner = WalletUtil.ethAddressToTron(callContractAddress(ContractMirrorConst.EMPTY_ADDRESS, "future_owner").toString());
        curve4PoolData.setFutureOwner(futureOwner);
        BigInteger transferOwnershipDeadline = callContractU256(ContractMirrorConst.EMPTY_ADDRESS, "transfer_ownership_deadline");
        curve4PoolData.setTransferOwnershipDeadline(transferOwnershipDeadline);
        isDirty = true;
        return true;
    }

    @Override
    public void updateBaseInfo(boolean isUsing, boolean isReady, boolean isAddExchangeContracts) {
        Curve4PoolData curve4PoolData = this.getVarCurve4PoolData();
        curve4PoolData.setUsing(isUsing);
        curve4PoolData.setReady(isReady);
        curve4PoolData.setAddExchangeContracts(isAddExchangeContracts);
        isDirty = true;
    }

    @Override
    protected void saveUpdateToCache() {
    }

    @Override
    protected HandleResult handleEvent1(String eventName, String[] topics, String data, HandleEventExtraData handleEventExtraData) {
        //TODO 等待具体合约代码
        HandleResult result;
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
    public <T> T getStatus() {
        return (T) getVarCurve4PoolData();
    }

    @Override
    public <T> T handleSpecialRequest(String method, String params) throws Exception {
        return null;
    }

    private HandleResult handleEventTokenExchangeUnderlying(String[] topics, String data, HandleEventExtraData handleEventExtraData) {
        log.info("TokenExchangeUnderlying not implements!");
        return HandleResult.genHandleFailMessage("TokenExchangeUnderlying not implements!");
    }

    private HandleResult handleEventTokenExchange(String[] topics, String data, HandleEventExtraData handleEventExtraData) {
        log.info("handleEventTokenExchange not implements!");
        return HandleResult.genHandleFailMessage("handleEventTokenExchange not implements!");
    }

    private HandleResult handleEventAddLiquidity(String[] topics, String data, HandleEventExtraData handleEventExtraData) {
        log.info("handleAddLiquidity not implements!");
        return HandleResult.genHandleFailMessage("handleAddLiquidity not implements!");
    }

    private HandleResult handleEventRemoveLiquidity(String[] topics, String data, HandleEventExtraData handleEventExtraData) {
        log.info("handleEventRemoveLiquidity not implements!");
        return HandleResult.genHandleFailMessage("handleEventRemoveLiquidity not implements!");
    }

    private HandleResult handleEventRemoveLiquidityOne(String[] topics, String data, HandleEventExtraData handleEventExtraData) {
        log.info("handleEventRemoveLiquidityOne not implements!");
        return HandleResult.genHandleFailMessage("handleEventRemoveLiquidityOne not implements!");
    }

    private HandleResult handleEventRemoveLiquidityImbalance(String[] topics, String data, HandleEventExtraData handleEventExtraData) {
        log.info("handleEventRemoveLiquidityImbalance not implements!");
        return HandleResult.genHandleFailMessage("handleEventRemoveLiquidityImbalance not implements!");
    }

    private HandleResult handleEventCommitNewAdmin(String[] topics, String data, HandleEventExtraData handleEventExtraData) {
        log.info("handleEventCommitNewAdmin not implements!");
        return HandleResult.genHandleFailMessage("handleEventCommitNewAdmin not implements!");
    }

    private HandleResult handleEventNewAdmin(String[] topics, String data, HandleEventExtraData handleEventExtraData) {
        log.info("handleEventNewAdmin not implements!");
        return HandleResult.genHandleFailMessage("handleEventNewAdmin not implements!");
    }

    private HandleResult handleEventNewFeeConverter(String[] topics, String data, HandleEventExtraData handleEventExtraData) {
        log.info("handleEventNewFeeConverter not implements!");
        return HandleResult.genHandleFailMessage("handleEventNewFeeConverter not implements!");
    }

    private HandleResult handleEventCommitNewFee(String[] topics, String data, HandleEventExtraData handleEventExtraData) {
        log.info("handleEventCommitNewFee not implements!");
        return HandleResult.genHandleFailMessage("handleEventCommitNewFee not implements!");
    }

    private HandleResult handleEventNewFee(String[] topics, String data, HandleEventExtraData handleEventExtraData) {
        log.info("handleEventNewFee not implements!");
        return HandleResult.genHandleFailMessage("handleEventNewFee not implements!");
    }

    private HandleResult handleEventRampA(String[] topics, String data, HandleEventExtraData handleEventExtraData) {
        log.info("handleEventRampA not implements!");
        return HandleResult.genHandleFailMessage("handleEventRampA not implements!");
    }

    private HandleResult handleEventStopRampA(String[] topics, String data, HandleEventExtraData handleEventExtraData) {
        log.info("handleEventStopRampA not implements!");
        return HandleResult.genHandleFailMessage("handleEventStopRampA not implements!");
    }
}
