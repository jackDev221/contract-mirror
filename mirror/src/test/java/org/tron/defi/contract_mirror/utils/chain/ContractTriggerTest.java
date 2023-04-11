package org.tron.defi.contract_mirror.utils.chain;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.tron.defi.contract_mirror.TestApplication;

@Slf4j
@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = TestApplication.class)
public class ContractTriggerTest {
    @Autowired
    TronContractTrigger tronContractTrigger;

    @Test
    public void getTrxBalanceTest() {
        long balance = tronContractTrigger.balance("TF5MekHgFz6neU7zTpX4h2tha5miPDUj3z");
        log.info(String.valueOf(balance));
        Assertions.assertEquals(0, balance);
    }
}
