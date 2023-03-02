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
import org.tron.defi.contract_mirror.core.ContractManager;

@Slf4j
@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = TestApplication.class)
public class TRC20Test {
    @Autowired
    private TokenConfigList tokenConfigList;
    @Autowired
    private ContractManager contractManager;

    @Test
    public void getDecimalsTest() {
        String usdtAddress = tokenConfigList.getTokens().get("USDT");
        TRC20 usdd = (TRC20) contractManager.registerContract(new TRC20(usdtAddress));
        Assertions.assertEquals(6, usdd.getDecimals());
    }

    @Test
    public void getSymbolTest() {
        String usdtAddress = tokenConfigList.getTokens().get("USDT");
        TRC20 usdd = (TRC20) contractManager.registerContract(new TRC20(usdtAddress));
        Assertions.assertEquals("USDT", usdd.getSymbol());
    }
}
