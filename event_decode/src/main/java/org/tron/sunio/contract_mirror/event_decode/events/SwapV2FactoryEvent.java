package org.tron.sunio.contract_mirror.event_decode.events;

import java.util.HashMap;
import java.util.Map;

import static org.tron.sunio.contract_mirror.event_decode.events.EventUtils.genSigNameMap;

public class SwapV2FactoryEvent {
    public static final String EVENT_NAME_PAIR_CREATED = "PairCreated";
    public static final String EVENT_NAME_NEW_PAIR_CREATED_BODY = "event PairCreated(address indexed token0, address indexed token1, address pair, uint);";

    public static Map<String, String> getSigMap() {
        Map<String, String> sigMap = new HashMap<>();
        Map<String, String> eventInfoMap = new HashMap<>();
        eventInfoMap.put(EVENT_NAME_PAIR_CREATED, EVENT_NAME_NEW_PAIR_CREATED_BODY);
        genSigNameMap(sigMap, eventInfoMap);
        return sigMap;
    }
}
