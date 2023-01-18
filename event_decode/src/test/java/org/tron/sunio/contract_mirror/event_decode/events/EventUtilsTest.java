package org.tron.sunio.contract_mirror.event_decode.events;

import org.junit.jupiter.api.Test;
import org.web3j.abi.EventValues;
import org.web3j.abi.datatypes.Address;
import org.web3j.abi.datatypes.Event;
import org.web3j.abi.datatypes.StaticArray;
import org.web3j.abi.datatypes.generated.StaticArray2;
import org.web3j.abi.datatypes.generated.Uint256;

import java.math.BigInteger;
import java.util.Arrays;

import static org.tron.sunio.contract_mirror.event_decode.events.Curve2PoolEvent.EVENT_NAME_REMOVE_LIQUIDITY_BODY;

public class EventUtilsTest {
    @Test
    public void testParseUtils() {
        Event event = EventUtils.parseEventString("event Transfer(address indexed from, address index to, uint256 value)");
        System.out.println(EventUtils.encodedEventSignature(event));
    }

    @Test
    public void testDecodeEvents() {
        String eventBody = "event RemoveLiquidityImbalance (address indexed provider, uint256[4] token_amounts, uint256[4] fees, uint256 invariant, uint256 token_supply)";
        String[] topics = new String[]{
                "0xb964b72f73f5ef5bf0fdc559b2fab9a7b12a39e47817a547f1f0aee47febd602",
                "0x000000000000000000000000bbc81d23ea2c3ec7e56d39296f0cbb648873a5d3"
        };
        String data = "0x0000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000002fc99f7000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000007aad49f3f340680000000000000000000000000000000000000000000000000000000000012c170000000000000000000000000000000000000000000000000000000000006a3300000000000000000000000000000000000000000000000000576c7a8fff5776000000000000000000000000000000000000000000059171af14b4b35ef3d0d800000000000000000000000000000000000000000004df34da7ff2387d641337";
        Event event = EventUtils.parseEventString(eventBody);
        System.out.println(EventUtils.encodedEventSignature(event));
        EventValues eventValues = EventUtils.getEventValue(eventBody,
                Arrays.asList(topics), data, true);
        System.out.println(new Address((String) eventValues.getIndexedValues().get(0).getValue()));
        StaticArray<Uint256> va = (StaticArray<Uint256>) eventValues.getNonIndexedValues().get(0);
        for (int i = 0; i < va.getValue().size(); i++) {
            System.out.println(va.getValue().get(i).getValue().longValue());
        }
        va = (StaticArray<Uint256>) eventValues.getNonIndexedValues().get(1);
        for (int i = 0; i < va.getValue().size(); i++) {
            System.out.println(va.getValue().get(i).getValue());
        }
    }
}
