package org.tron.sunio.contract_mirror.event_decode.events;

import org.junit.jupiter.api.Test;
import org.web3j.abi.datatypes.Event;

public class EventUtilsTest {
    @Test
    public void testParseUtils(){
        Event event =  EventUtils.parseEventString("event Transfer(address indexed from, address indexed to, uint256 value)");
        System.out.println(EventUtils.encodedEventSignature(event));
    }
}
