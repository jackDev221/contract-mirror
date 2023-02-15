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

import static org.tron.defi.contract_mirror.common.ContractType.CURVE_3POOL;
import static org.tron.defi.contract_mirror.common.ContractType.CURVE_4POOL;

@Slf4j
@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = TestApplication.class)
public class Curve4PoolTest {
    @Autowired
    private ContractConfigList contractConfigList;
    @Autowired
    private ContractManager contractManager;

    private ContractConfigList.ContractConfig config;

    @BeforeEach
    public void setUp() {
        for (ContractConfigList.ContractConfig contractConfig : contractConfigList.getContracts()) {
            if (CURVE_3POOL == contractConfig.getType()) {
                contractManager.initCurve(contractConfig.getAddress(), contractConfig.getType());
            } else if (CURVE_4POOL == contractConfig.getType()) {
                config = contractConfig;
                break;
            }
        }
    }

    @Test
    public void initTest() {
        Assertions.assertNotNull(config);
        Curve4Pool pool
            = (Curve4Pool) contractManager.registerContract(new Curve4Pool(config.getAddress()));
        Assertions.assertTrue(pool.init());
        log.info(pool.info());
    }
}
