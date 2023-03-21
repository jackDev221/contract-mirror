package org.tron.sunio.contract_mirror.event_decode.events;

import java.util.HashMap;
import java.util.Map;

import static org.tron.sunio.contract_mirror.event_decode.events.EventUtils.genSigNameMap;

public class SwapV1Event {
    public static final String EVENT_NAME_TRANSFER = "Transfer";
    public static final String EVENT_NAME_TRANSFER_BODY = "event Transfer(address indexed from, address indexed to, uint256 value);";
    public static final String EVENT_NAME_TOKEN_PURCHASE = "TokenPurchase";
    public static final String EVENT_NAME_TOKEN_PURCHASE_BODY = "event TokenPurchase(address indexed buyer, uint256 indexed trx_sold, uint256 indexed tokens_bought)";
    public static final String EVENT_NAME_TRX_PURCHASE = "TrxPurchase";
    public static final String EVENT_NAME_TRX_PURCHASE_BODY = "event TrxPurchase(address indexed buyer, uint256 indexed tokens_sold, uint256 indexed trx_bought);";
    public static final String EVENT_NAME_TOKEN_TO_TOKEN = "TokenToToken";
    public static final String EVENT_NAME_TOKEN_TO_TOKEN_BODY = "event TokenToToken(address indexed buyer, address from_exchangne, address to_exchange, uint256 from_value, uint256 to_value);";
    public static final String EVENT_NAME_ADD_LIQUIDITY = "AddLiquidity";
    public static final String EVENT_NAME_ADD_LIQUIDITY_BODY = "event AddLiquidity(address indexed provider, uint256 indexed trx_amount, uint256 indexed token_amount);";
    public static final String EVENT_NAME_REMOVE_LIQUIDITY = "RemoveLiquidity";
    public static final String EVENT_NAME_REMOVE_LIQUIDITY_BODY = "event RemoveLiquidity(address indexed provider, uint256 indexed trx_amount, uint256 indexed token_amount);";
    public static final String EVENT_NAME_SNAPSHOT = "Snapshot";
    public static final String EVENT_NAME_SNAPSHOT_BODY = "event Snapshot(address indexed operator, uint256 indexed trx_balance, uint256 indexed token_balance);";
    public static final String EVENT_NAME_ADMIN_FEE_MINT = "AdminFeeMint";
    public static final String EVENT_NAME_ADMIN_FEE_MINT_BODY = "event AdminFeeMint(address indexed from, address indexed to, uint256 value);";

    public static Map<String, String> getSigMap() {
        Map<String, String> sigMap = new HashMap<>();
        Map<String, String> eventInfoMap = new HashMap<>();
        eventInfoMap.put(EVENT_NAME_TRANSFER, EVENT_NAME_TRANSFER_BODY);
        eventInfoMap.put(EVENT_NAME_TOKEN_PURCHASE, EVENT_NAME_TOKEN_PURCHASE_BODY);
        eventInfoMap.put(EVENT_NAME_TRX_PURCHASE, EVENT_NAME_TRX_PURCHASE_BODY);
        eventInfoMap.put(EVENT_NAME_TOKEN_TO_TOKEN, EVENT_NAME_TOKEN_TO_TOKEN_BODY);
        eventInfoMap.put(EVENT_NAME_ADD_LIQUIDITY, EVENT_NAME_ADD_LIQUIDITY_BODY);
        eventInfoMap.put(EVENT_NAME_REMOVE_LIQUIDITY, EVENT_NAME_REMOVE_LIQUIDITY_BODY);
        eventInfoMap.put(EVENT_NAME_SNAPSHOT, EVENT_NAME_SNAPSHOT_BODY);
        eventInfoMap.put(EVENT_NAME_ADMIN_FEE_MINT, EVENT_NAME_ADMIN_FEE_MINT_BODY);
        genSigNameMap(sigMap, eventInfoMap);
        return sigMap;
    }
}
