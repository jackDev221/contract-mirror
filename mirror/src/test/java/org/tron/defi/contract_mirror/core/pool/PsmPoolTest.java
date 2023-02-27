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

import static org.tron.defi.contract_mirror.common.ContractType.PSM_POOL;

@Slf4j
@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = TestApplication.class)
public class PsmPoolTest {
    @Autowired
    private ContractConfigList contractConfigList;
    @Autowired
    private ContractManager contractManager;
    private ContractConfigList.ContractConfig config;

    @BeforeEach
    public void setUp() {
        for (ContractConfigList.ContractConfig contractConfig : contractConfigList.getContracts()) {
            if (PSM_POOL == contractConfig.getType()) {
                config = contractConfig;
                break;
            }
        }
    }

    @Test
    public void initTest() {
        Assertions.assertNotNull(config);
        PsmPool pool = (PsmPool) contractManager.registerContract(new PsmPool(config.getAddress(),
                                                                              config.getPolyAddress()));
        Assertions.assertDoesNotThrow(() -> pool.init());
        log.info(pool.info());
    }
}
