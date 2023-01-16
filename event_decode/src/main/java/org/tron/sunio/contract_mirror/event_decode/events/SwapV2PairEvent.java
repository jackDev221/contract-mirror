package org.tron.sunio.contract_mirror.event_decode.events;

import java.util.HashMap;
import java.util.Map;

import static org.tron.sunio.contract_mirror.event_decode.events.EventUtils.genSigNameMap;

public class SwapV2PairEvent {
    public static final String EVENT_NAME_TRANSFER= "Transfer";
    public static final String EVENT_NAME_TRANSFER_BODY= "event Transfer(address indexed from, address indexed to, uint256 value);";
    public static final String EVENT_NAME_NEW_MINT = "Mint";
    public static final String EVENT_NAME_NEW_MINT_BODY = "event Mint(address indexed sender, uint amount0, uint amount1);";
    public static final String EVENT_NAME_NEW_BURN = "Burn";
    public static final String EVENT_NAME_NEW_BURN_BODY = "event Burn(address indexed sender, uint amount0, uint amount1, address indexed to);";
    public static final String EVENT_NAME_NEW_SWAP = "Swap";
    public static final String EVENT_NAME_NEW_SWAP_BODY = "event Swap(address indexed sender,uint amount0In,uint amount1In,uint amount0Out,uint amount1Out,address indexed to);";
    public static final String EVENT_NAME_NEW_SYNC = "Sync";
    public static final String EVENT_NAME_NEW_SYNC_BODY = "event Sync(uint112 reserve0, uint112 reserve1);";

    public static Map<String, String> getSigMap() {
        Map<String, String> sigMap = new HashMap<>();
        Map<String, String> eventInfoMap = new HashMap<>();
        eventInfoMap.put(EVENT_NAME_TRANSFER, EVENT_NAME_TRANSFER_BODY);
        eventInfoMap.put(EVENT_NAME_NEW_MINT, EVENT_NAME_NEW_MINT_BODY);
        eventInfoMap.put(EVENT_NAME_NEW_BURN, EVENT_NAME_NEW_BURN_BODY);
        eventInfoMap.put(EVENT_NAME_NEW_SWAP, EVENT_NAME_NEW_SWAP_BODY);
        eventInfoMap.put(EVENT_NAME_NEW_SYNC, EVENT_NAME_NEW_SYNC_BODY);
        genSigNameMap(sigMap, eventInfoMap);
        return sigMap;
    }
}
