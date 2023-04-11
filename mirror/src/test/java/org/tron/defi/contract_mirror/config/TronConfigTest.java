package org.tron.defi.contract_mirror.config;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.tron.defi.contract_mirror.TestApplication;
import org.tron.sunapi.SunNetwork;

@Slf4j
@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = TestApplication.class)
public class TronConfigTest {
    @Autowired
    private TronConfig tronConfig;
    @Autowired
    private SunNetwork sunNetwork;

    @Test
    void tronConfigTest() {
        Assertions.assertNotNull(tronConfig);
        log.info(tronConfig.toString());
        Assertions.assertNotNull(sunNetwork);
    }
}
