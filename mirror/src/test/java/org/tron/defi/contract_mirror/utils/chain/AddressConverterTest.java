package org.tron.defi.contract_mirror.utils.chain;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

@Slf4j
public class AddressConverterTest {
    private final String ethAddress = "8dfa9ff69ae191ce8c5026297f45eb1ac18c122e";
    private final String tronBase58Address = "TNuvWpwfWnqhfR7NgDMtZJ1Wdrkv8CadSZ";

    @Test
    void testEthToTronBase58Address() {
        Assertions.assertEquals(tronBase58Address,
                                AddressConverter.EthToTronBase58Address(ethAddress));
    }

    @Test
    void testTronBase58ToEthAddress() {
        Assertions.assertEquals(ethAddress,
                                AddressConverter.TronBase58ToEthAddress(tronBase58Address));
    }
}
