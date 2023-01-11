package org.tron.sunio.contract_mirror.event_decode.events;

import java.util.HashMap;
import java.util.Map;

import static org.tron.sunio.contract_mirror.event_decode.events.EventUtils.genSigNameMap;

public class SwapV1FactoryEvent {
    public static final String EVENT_NAME_NEW_EXCHANGE = "NewExchange";
    public static final String EVENT_NAME_NEW_EXCHANGE_BODY= "event NewExchange(address indexed token, address indexed exchange);";
    public static final String EVENT_NAME_FEE_TO = "NewFeeTo";
    public static final String EVENT_NAME_FEE_TO_BODY = "event NewFeeTo(address feeTo);";
    public static final String EVENT_NAME_FEE_RATE= "NewFeeRate";
    public static final String EVENT_NAME_FEE_RATE_BODY = "event NewFeeRate(uint256 feeTo);";

    public static Map<String, String> getSigMap() {
        Map<String, String> sigMap = new HashMap<>();
        Map<String, String> eventInfoMap = new HashMap<>();
        eventInfoMap.put(EVENT_NAME_NEW_EXCHANGE, EVENT_NAME_NEW_EXCHANGE_BODY);
        eventInfoMap.put(EVENT_NAME_FEE_TO, EVENT_NAME_FEE_TO_BODY);
        eventInfoMap.put(EVENT_NAME_FEE_RATE, EVENT_NAME_FEE_RATE_BODY);
        genSigNameMap(sigMap, eventInfoMap);
        return sigMap;
    }

}
