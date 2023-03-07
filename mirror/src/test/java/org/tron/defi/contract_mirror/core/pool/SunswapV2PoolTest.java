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
import org.tron.defi.contract_mirror.dao.BlockInfo;
import org.tron.defi.contract_mirror.dao.KafkaMessage;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Slf4j
@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = TestApplication.class)
public class SunswapV2PoolTest {
    @Autowired
    private ContractManager contractManager;
    private SunswapV2Pool pool;

    @Test
    public void initTest() {
        Assertions.assertDoesNotThrow(() -> pool.init());
    }

    @Test
    public void getReservesFromChainTest() {
        List<BigInteger> reserves = pool.getReservesFromChain();
        log.info(reserves.toString());
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
        topics[1] = "0000000000000000000000000000000000000000000000000000000000000000";
        topics[2] = "00000000000000000000000095ea94607f769b5b612dba2ad9a4ef01a1e04c87";
        rawData.setTopics(topics);
        rawData.setData("00000000000000000000000000000000000000000000000000000042538f812f");
        log.setRawData(rawData);
        Assertions.assertDoesNotThrow(() -> pool.onEvent(new KafkaMessage<>(log), 86400000));
    }

    @BeforeEach
    void setUp() {
        final String address = "TR4fHizLc7xCy6v1UVdTqLxYzTW1QHCds6";
        pool = (SunswapV2Pool) contractManager.registerContract(new SunswapV2Pool(address));
        Contract token0 = (Contract) pool.getToken0();
        Contract token1 = (Contract) pool.getToken1();
        pool.setTokens(new ArrayList<>(Arrays.asList(token0, token1)));
    }
}
