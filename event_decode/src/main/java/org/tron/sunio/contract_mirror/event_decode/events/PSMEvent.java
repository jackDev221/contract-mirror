package org.tron.sunio.contract_mirror.event_decode.events;

import java.util.HashMap;
import java.util.Map;

import static org.tron.sunio.contract_mirror.event_decode.events.EventUtils.genSigNameMap;

public class PSMEvent {
    public static final String EVENT_NAME_FILE = "File";
    public static final String EVENT_NAME_FILE_BODY = "event File(bytes32 indexed what, uint256 data);";
    public static final String EVENT_NAME_SELL_GEM = "SellGem";
    public static final String EVENT_NAME_SELL_GEM_BODY = "event SellGem(address indexed owner, uint256 value, uint256 fee);";
    public static final String EVENT_NAME_BUY_GEM = "BuyGem";
    public static final String EVENT_NAME_BUY_GEM_BODY = "event BuyGem(address indexed owner, uint256 value, uint256 fee);";

    public static Map<String, String> getSigMap() {
        Map<String, String> sigMap = new HashMap<>();
        Map<String, String> eventInfoMap = new HashMap<>();
        eventInfoMap.put(EVENT_NAME_FILE, EVENT_NAME_FILE_BODY);
        eventInfoMap.put(EVENT_NAME_SELL_GEM, EVENT_NAME_SELL_GEM_BODY);
        eventInfoMap.put(EVENT_NAME_BUY_GEM, EVENT_NAME_BUY_GEM_BODY);
        genSigNameMap(sigMap, eventInfoMap);
        return sigMap;
    }
}
