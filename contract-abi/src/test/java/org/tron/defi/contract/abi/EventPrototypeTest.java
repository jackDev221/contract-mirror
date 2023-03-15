package org.tron.defi.contract.abi;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class EventPrototypeTest {
    @Test
    public void constructorTest() {
        EventPrototype prototype = new EventPrototype("AddLiquidity",
                                                      "address indexed,uint256[3],uint256[3]," +
                                                      "uint256,uint256");
        Assertions.assertEquals("AddLiquidity(address,uint256[3],uint256[3],uint256,uint256)",
                                prototype.getRawSignature());
        Assertions.assertEquals("423f6495a08fc652425cf4ed0d1f9e37e571d9b9529b1c1c23cce780b2e7df0d",
                                prototype.getSignature());
    }
}
