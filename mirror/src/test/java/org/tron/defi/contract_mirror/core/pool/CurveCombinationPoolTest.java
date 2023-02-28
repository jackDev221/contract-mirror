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
import static org.tron.defi.contract_mirror.common.ContractType.CURVE_COMBINATION_4POOL;

@Slf4j
@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = TestApplication.class)
public class CurveCombinationPoolTest {
    @Autowired
    private ContractConfigList contractConfigList;
    @Autowired
    private ContractManager contractManager;

    private ContractConfigList.ContractConfig config;

    @Test
    public void initTest() {
        Assertions.assertNotNull(config);
        CurveCombinationPool pool
            = (CurveCombinationPool) contractManager.registerContract(new CurveCombinationPool(
            config.getAddress(),
            PoolType.CURVE_COMBINATION4));
        Assertions.assertDoesNotThrow(() -> pool.init());
        log.info(pool.info());
    }

    @BeforeEach
    public void setUp() {
        for (ContractConfigList.ContractConfig contractConfig : contractConfigList.getContracts()) {
            if (CURVE_3POOL == contractConfig.getType()) {
                contractManager.initCurve(contractConfig.getAddress(), contractConfig.getType());
            } else if (CURVE_COMBINATION_4POOL == contractConfig.getType()) {
                config = contractConfig;
                break;
            }
        }
    }
}
