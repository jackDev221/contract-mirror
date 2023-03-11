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
import org.tron.defi.contract_mirror.dao.BlockInfo;
import org.tron.defi.contract_mirror.dao.KafkaMessage;

import static org.tron.defi.contract_mirror.core.ContractType.CURVE_2POOL;

@Slf4j
@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = TestApplication.class)
public class CurvePoolTest {
    @Autowired
    private ContractConfigList contractConfigList;
    @Autowired
    private ContractManager contractManager;
    private ContractConfigList.ContractConfig config;
    private CurvePool pool;

    @Test
    public void handleTokenExchangeTest() {
        Assertions.assertDoesNotThrow(() -> pool.init());

        ContractLog log = new ContractLog();
        log.setTimeStamp(pool.getTimestamp2() + 1L);
        BlockInfo blockInfo = pool.getLastBlock();
        if (null != blockInfo) {
            log.setBlockNumber(blockInfo.getBlockNumber());
            log.setBlockHash(blockInfo.getBlockHash());
        } else {
            log.setBlockNumber(49049544L);
            log.setBlockHash("0000000002ec6fc8c40d97daa9a7fe05b85a22e63f3971a25a4b1cf98a8e2f70");
        }
        log.setRemoved(false);
        ContractLog.RawData rawData = new ContractLog.RawData();
        String[] topics = new String[2];
        topics[0] = "8b3e96f2b889fa771c53c981b40daf005f63f637f1869f707052d15a3dd97140";
        topics[1] = "000000000000000000000000a2c2426d23bb43809e6eba1311afddde8d45f5d8";
        rawData.setTopics(topics);
        rawData.setData(
            "000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000002fa90a32a2149a0000000000000000000000000000000000000000000000000000000000000000000100000000000000000000000000000000000000000000000000000000342c0151");
        log.setRawData(rawData);

        Assertions.assertDoesNotThrow(() -> pool.onEvent(new KafkaMessage<>(log), 86400000));
    }

    @Test
    public void initTest() {
        Assertions.assertDoesNotThrow(() -> pool.init());
        log.info(pool.info());
    }

    @BeforeEach
    public void setUp() {
        for (ContractConfigList.ContractConfig contractConfig : contractConfigList.getContracts()) {
            if (CURVE_2POOL == contractConfig.getType()) {
                config = contractConfig;
                break;
            }
        }
        Assertions.assertNotNull(config);
        log.info(config.toString());
        pool = (CurvePool) contractManager.registerContract(new CurvePool(config.getAddress(),
                                                                          PoolType.convertFromContractType(
                                                                              config.getType())));
    }
}
