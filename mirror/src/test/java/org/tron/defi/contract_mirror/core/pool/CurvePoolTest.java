package org.tron.defi.contract_mirror.core.pool;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.tron.defi.contract.log.ContractLog;
import org.tron.defi.contract_mirror.TestApplication;
import org.tron.defi.contract_mirror.config.ContractConfigList;
import org.tron.defi.contract_mirror.core.ContractManager;
import org.tron.defi.contract_mirror.dao.KafkaMessage;

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
    public void handleTokenExchangeTest() {
        ContractLog log = new ContractLog();
        log.setTimeStamp(1677760023000L);
        log.setUniqueId("675471ca7791277f0cbdac9fda6a17e8053e0c0c2b1473ce3a6c9e7fd86c22eb_3");
        log.setTransactionId("675471ca7791277f0cbdac9fda6a17e8053e0c0c2b1473ce3a6c9e7fd86c22eb");
        log.setContractAddress("TNTfaTpkdd4AQDeqr8SGG7tgdkdjdhbP5c");
        log.setOriginAddress("TQooBX9o8iSSprLWW96YShBogx7Uwisuim");
        log.setCreatorAddress("TFUerjaCNrEmXbBuW9oG42U6KLhzzFGk5o");
        log.setBlockNumber(49049544L);
        log.setBlockHash("0000000002ec6fc8c40d97daa9a7fe05b85a22e63f3971a25a4b1cf98a8e2f70");
        log.setRemoved(false);
        log.setLatestSolidifiedBlockNumber(49049525L);
        ContractLog.RawData rawData = new ContractLog.RawData();
        rawData.setAddress("8903573f4c59704f3800e697ac333d119d142da9");
        String[] topics = new String[2];
        topics[0] = "8b3e96f2b889fa771c53c981b40daf005f63f637f1869f707052d15a3dd97140";
        topics[1] = "000000000000000000000000a2c2426d23bb43809e6eba1311afddde8d45f5d8";
        rawData.setTopics(topics);
        rawData.setData(
            "000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000002fa90a32a2149a0000000000000000000000000000000000000000000000000000000000000000000100000000000000000000000000000000000000000000000000000000342c0151");
        log.setRawData(rawData);


        CurvePool pool
            = (CurvePool) contractManager.registerContract(new CurvePool(log.getContractAddress(),
                                                                         PoolType.CURVE2));
        Assertions.assertDoesNotThrow(() -> pool.init());
        Assertions.assertDoesNotThrow(() -> pool.onEvent(new KafkaMessage<ContractLog>(log),
                                                         86400000));
    }

    @Test
    public void initTest() {
        Assertions.assertNotNull(config);
        log.info(config.toString());
        CurvePool pool
            = (CurvePool) contractManager.registerContract(new CurvePool(config.getAddress(),
                                                                         PoolType.convertFromContractType(
                                                                             config.getType())));
        Assertions.assertDoesNotThrow(() -> pool.init());
        log.info(pool.info());
    }
}
