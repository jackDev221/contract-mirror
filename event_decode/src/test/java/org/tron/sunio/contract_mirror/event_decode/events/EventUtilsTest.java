package org.tron.sunio.contract_mirror.event_decode.events;

import org.junit.jupiter.api.Test;
import org.web3j.abi.datatypes.Event;

public class EventUtilsTest {
    @Test
    public void testParseUtils(){
        Event event =  EventUtils.parseEventString("event RemoveLiquidityImbalance( address indexed provider, uint256[2] token_amounts, uint256[2] fees, uint256 invariant, uint256 token_supply);");
        System.out.println(EventUtils.encodedEventSignature(event));
    }
}
