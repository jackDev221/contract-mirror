package org.tron.defi.contract_mirror.core.token;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.tron.defi.contract_mirror.TestApplication;
import org.tron.defi.contract_mirror.config.TokenConfigList;


@Slf4j
@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = TestApplication.class)
public class TRXTest {
    @Autowired
    private TokenConfigList tokenConfigList;

    @Test
    public void getInstanceTest() {
        TRX trx = TRX.getInstance();
        Assertions.assertEquals(trx.getDecimals(), 6);
        Assertions.assertEquals(trx.getSymbol(), "TRX");
        Assertions.assertEquals(trx.getAddress(), tokenConfigList.getTokens().get("TRX"));
    }
}
