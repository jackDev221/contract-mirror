package org.tron.defi.contract.abi;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class EventPrototypeTest {
    @Test
    public void constructorTest() {
        EventPrototype prototype = new EventPrototype("Transfer",
                                                      "address indexed,address indexed,uint256");
        Assertions.assertEquals("Transfer(address,address,uint256)", prototype.getRawSignature());
        Assertions.assertEquals("ddf252ad1be2c89b69c2b068fc378daa952ba7f163c4a11628f55a4df523b3ef",
                                prototype.getSignature());
    }
}
