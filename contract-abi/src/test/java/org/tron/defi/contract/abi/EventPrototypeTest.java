package org.tron.defi.contract.abi;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class EventPrototypeTest {
    @Test
    public void constructorTest() {
        EventPrototype prototype = new EventPrototype("Snapshot",
                                                      "address indexed,uint256 indexed,uint256 " +
                                                      "indexed");
        Assertions.assertEquals("Snapshot(address,uint256,uint256)", prototype.getRawSignature());
        Assertions.assertEquals("cc7244d3535e7639366f8c5211527112e01de3ec7449ee3a6e66b007f4065a70",
                                prototype.getSignature());
    }
}
