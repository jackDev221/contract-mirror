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

import static org.tron.defi.contract_mirror.common.ContractType.CURVE_2POOL;

@Slf4j
@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = TestApplication.class)
public class CurvePoolTest {
    @Autowired
    private ContractConfigList contractConfigList;
    @Autowired
    private ContractManager contractManager;

    private ContractConfigList.ContractConfig config;

    @BeforeEach
    public void setUp() {
        for (ContractConfigList.ContractConfig contractConfig : contractConfigList.getContracts()) {
            if (CURVE_2POOL == contractConfig.getType()) {
                config = contractConfig;
                break;
            }
        }
    }

    @Test
    public void initTest() {
        Assertions.assertNotNull(config);
        CurvePool pool
            = (CurvePool) contractManager.registerContract(new CurvePool(config.getAddress(),
                                                                         PoolType.convertFromContractType(
                                                                             config.getType())));
        Assertions.assertTrue(pool.init());
        log.info(pool.info());
    }
}
