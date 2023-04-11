package org.tron.defi.contract_mirror.config;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.tron.defi.contract_mirror.TestApplication;

import java.util.Map;

@Slf4j
@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = TestApplication.class)
public class PriceCenterConfigTest {
    @Autowired
    private PriceCenterConfig priceCenterConfig;

    @Test
    public void priceCenterConfigTest() {
        Assertions.assertNotNull(priceCenterConfig);
        log.info(priceCenterConfig.toString());
        Assertions.assertEquals("c.tronlink.org", priceCenterConfig.getServer());
        Assertions.assertEquals("/v1/cryptocurrency/getprice", priceCenterConfig.getUri());
        Map<String, String> params = priceCenterConfig.getParams();
        Assertions.assertNotNull(params);
        Assertions.assertEquals(1, params.size());
        Assertions.assertTrue(params.containsKey("convert"));
        Assertions.assertEquals("USD", params.get("convert"));
    }
}
