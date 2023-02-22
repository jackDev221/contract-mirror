package org.tron.sunio.contract_mirror.event_decode.events;

import java.util.HashMap;
import java.util.Map;

import static org.tron.sunio.contract_mirror.event_decode.events.EventUtils.genSigNameMap;

public class Curve3PoolEvent {
    public static final String EVENT_NAME_TRANSFER = "Transfer";
    public static final String EVENT_NAME_TRANSFER_BODY = "event Transfer(address indexed from, address indexed to, uint256 value);";
    public static final String EVENT_NAME_TOKEN_EXCHANGE = "TokenExchange";
    public static final String EVENT_NAME_TOKEN_EXCHANGE_BODY = "event TokenExchange(address indexed buyer, int128 sold_id,uint256 tokens_sold,int128 bought_id,uint256 tokens_bought);";
    public static final String EVENT_NAME_TOKEN_EXCHANGE_UNDERLING = "TokenExchangeUnderlying";
    public static final String EVENT_NAME_TOKEN_EXCHANGE_UNDERLING_BODY = "event TokenExchangeUnderlying(address indexed buyer, int128 sold_id,uint256 tokens_sold,int128 bought_id,uint256 tokens_bought);";
    public static final String EVENT_NAME_ADD_LIQUIDITY = "AddLiquidity";
    public static final String EVENT_NAME_ADD_LIQUIDITY_BODY = "event AddLiquidity(address indexed provider, uint256[3] token_amounts, uint256[3] fees, uint256 invariant, uint256 token_supply);";
    public static final String EVENT_NAME_REMOVE_LIQUIDITY = "RemoveLiquidity";
    public static final String EVENT_NAME_REMOVE_LIQUIDITY_BODY = "event RemoveLiquidity(address indexed provider, uint256[3] token_amounts, uint256[3] fees, uint256 token_supply);";
    public static final String EVENT_NAME_REMOVE_LIQUIDITY_ONE = "RemoveLiquidityOne";
    public static final String EVENT_NAME_REMOVE_LIQUIDITY_ONE_BODY = "event RemoveLiquidityOne(address indexed provider, uint256 token_amount, uint256 coin_amount);";
    public static final String EVENT_NAME_REMOVE_LIQUIDITY_IM_BALANCE = "RemoveLiquidityImbalance";
    public static final String EVENT_NAME_REMOVE_LIQUIDITY_IM_BALANCE_BODY = "event RemoveLiquidityImbalance(address indexed provider, uint256[3] token_amounts, uint256[3] fees, uint256 invariant, uint256 token_supply);";
    public static final String EVENT_NAME_COMMIT_NEW_ADMIN = "CommitNewAdmin";
    public static final String EVENT_NAME_COMMIT_NEW_ADMIN_BODY = "event CommitNewAdmin(uint256 indexed deadline, address indexed admin);";
    public static final String EVENT_NAME_NEW_ADMIN = "NewAdmin";
    public static final String EVENT_NAME_NEW_ADMIN_BODY = "event NewAdmin(address indexed admin);";
    public static final String EVENT_NAME_NEW_FEE_CONVERTER = "NewFeeConverter";
    public static final String EVENT_NAME_NEW_FEE_CONVERTER_BODY = "event NewFeeConverter(address indexed fee_converter);";
    public static final String EVENT_NAME_COMMIT_NEW_FEE = "CommitNewFee";
    public static final String EVENT_NAME_COMMIT_NEW_FEE_BODY = "event CommitNewFee(uint256 indexed deadline, uint256 fee, uint256 admin_fee);";
    public static final String EVENT_NAME_NEW_FEE = "NewFee";
    public static final String EVENT_NAME_NEW_FEE_BODY = "event NewFee(uint256 fee, uint256 admin_fee);";
    public static final String EVENT_NAME_RAMP_A = "RampA";
    public static final String EVENT_NAME_RAMP_A_BODY = "event RampA(uint256 old_A, uint256 new_A, uint256 initial_time, uint256 future_time);";
    public static final String EVENT_NAME_STOP_RAMP_A = "StopRampA";
    public static final String EVENT_NAME_STOP_RAMP_A_BODY = "event StopRampA(uint256 A, uint256 t);";

    public static Map<String, String> getSigMap() {
        Map<String, String> sigMap = new HashMap<>();
        Map<String, String> eventInfoMap = new HashMap<>();
        eventInfoMap.put(EVENT_NAME_TRANSFER, EVENT_NAME_TRANSFER_BODY);
        eventInfoMap.put(EVENT_NAME_TOKEN_EXCHANGE_UNDERLING, EVENT_NAME_TOKEN_EXCHANGE_UNDERLING_BODY);
        eventInfoMap.put(EVENT_NAME_TOKEN_EXCHANGE, EVENT_NAME_TOKEN_EXCHANGE_BODY);
        eventInfoMap.put(EVENT_NAME_ADD_LIQUIDITY, EVENT_NAME_ADD_LIQUIDITY_BODY);
        eventInfoMap.put(EVENT_NAME_REMOVE_LIQUIDITY, EVENT_NAME_REMOVE_LIQUIDITY_BODY);
        eventInfoMap.put(EVENT_NAME_REMOVE_LIQUIDITY_ONE, EVENT_NAME_REMOVE_LIQUIDITY_ONE_BODY);
        eventInfoMap.put(EVENT_NAME_REMOVE_LIQUIDITY_IM_BALANCE, EVENT_NAME_REMOVE_LIQUIDITY_IM_BALANCE_BODY);
        eventInfoMap.put(EVENT_NAME_COMMIT_NEW_ADMIN, EVENT_NAME_COMMIT_NEW_ADMIN_BODY);
        eventInfoMap.put(EVENT_NAME_NEW_ADMIN, EVENT_NAME_NEW_ADMIN_BODY);
        eventInfoMap.put(EVENT_NAME_NEW_FEE_CONVERTER, EVENT_NAME_NEW_FEE_CONVERTER_BODY);
        eventInfoMap.put(EVENT_NAME_COMMIT_NEW_FEE, EVENT_NAME_COMMIT_NEW_FEE_BODY);
        eventInfoMap.put(EVENT_NAME_NEW_FEE, EVENT_NAME_NEW_FEE_BODY);
        eventInfoMap.put(EVENT_NAME_RAMP_A, EVENT_NAME_RAMP_A_BODY);
        eventInfoMap.put(EVENT_NAME_STOP_RAMP_A, EVENT_NAME_STOP_RAMP_A_BODY);
        genSigNameMap(sigMap, eventInfoMap);
        return sigMap;
    }
}
