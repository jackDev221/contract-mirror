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
import org.tron.defi.contract_mirror.core.Contract;
import org.tron.defi.contract_mirror.core.ContractManager;
import org.tron.defi.contract_mirror.core.token.TRC20;
import org.tron.defi.contract_mirror.core.token.TRX;
import org.tron.defi.contract_mirror.dao.BlockInfo;
import org.tron.defi.contract_mirror.dao.KafkaMessage;

import java.util.ArrayList;
import java.util.Arrays;

@Slf4j
@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = TestApplication.class)
public class SunswapV1PoolTest {
    @Autowired
    private ContractManager contractManager;
    private SunswapV1Pool pool;

    @Test
    public void initTest() {
        Assertions.assertDoesNotThrow(() -> pool.init());
    }

    @Test
    public void onTransferEventTest() {
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
        String[] topics = new String[3];
        topics[0] = "ddf252ad1be2c89b69c2b068fc378daa952ba7f163c4a11628f55a4df523b3ef";
        topics[1] = "00000000000000000000000026dde7f662c7d1c654fc9a9e1f8e52d72f817227";
        topics[2] = "0000000000000000000000000000000000000000000000000000000000000000";
        rawData.setTopics(topics);
        rawData.setData("0000000000000000000000000000000000000000000000000000000034edce00");
        log.setRawData(rawData);
        Assertions.assertDoesNotThrow(() -> pool.onEvent(new KafkaMessage<>(log), 86400000));
    }

    @BeforeEach
    void setUp() {
        contractManager.initTRX();
        final String poolAddress = "TStojTQUzpUatNY1xtc5uD2SoCDcn4RD5Z";
        final String tokenAddress = "TYr8aaM1nyEKi9wvg4h1r5dPXfuEkF68Dn";
        pool = contractManager.registerOrReplacePool(new SunswapV1Pool(poolAddress),
                                                     SunswapV1Pool.class);
        Contract token = contractManager.registerContract(new TRC20(tokenAddress));
        pool.setTokens(new ArrayList<>(Arrays.asList(TRX.getInstance(), token)));
    }
}
