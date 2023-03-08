package org.tron.defi.contract_mirror.core.pool;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.tron.defi.contract_mirror.TestApplication;
import org.tron.defi.contract_mirror.config.ContractConfigList;
import org.tron.defi.contract_mirror.core.ContractManager;

import static org.tron.defi.contract_mirror.common.ContractType.WTRX_TOKEN;

@Slf4j
@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = TestApplication.class)
public class WTRXTest {
    @Autowired
    private ContractConfigList contractConfigList;
    @Autowired
    private ContractManager contractManager;
    private WTRX wtrx;

    @Test
    public void initTest() {
        Assertions.assertNotNull(wtrx);
        Assertions.assertDoesNotThrow(() -> wtrx.init());
    }

    @BeforeEach
    public void setUp() {
        contractManager.initTRX();
        for (ContractConfigList.ContractConfig config : contractConfigList.getContracts()) {
            if (WTRX_TOKEN == config.getType()) {
                log.info(config.toString());
                wtrx = (WTRX) contractManager.registerContract(new WTRX(config.getAddress()));
            }
        }
    }
}
