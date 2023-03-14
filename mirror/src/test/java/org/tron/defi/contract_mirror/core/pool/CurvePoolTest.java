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
import org.tron.defi.contract_mirror.core.Contract;
import org.tron.defi.contract_mirror.core.ContractManager;
import org.tron.defi.contract_mirror.core.token.IToken;
import org.tron.defi.contract_mirror.dao.BlockInfo;
import org.tron.defi.contract_mirror.dao.KafkaMessage;
import org.tron.defi.contract_mirror.utils.MethodUtil;

import java.math.BigInteger;
import java.util.List;

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

    private static BigInteger getD(CurvePool pool, List<BigInteger> xp, BigInteger A) {
        try {
            return (BigInteger) MethodUtil.getNonAccessibleMethod(CurvePool.class,
                                                                  "getD",
                                                                  List.class,
                                                                  BigInteger.class)
                                          .invoke(pool, xp, A);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private static BigInteger getDeltaY(CurvePool pool, int i, int j, BigInteger amountIn) {
        try {
            return (BigInteger) MethodUtil.getNonAccessibleMethod(CurvePool.class,
                                                                  "getDeltaY",
                                                                  int.class,
                                                                  int.class,
                                                                  BigInteger.class,
                                                                  long.class)
                                          .invoke(pool,
                                                  i,
                                                  j,
                                                  amountIn,
                                                  System.currentTimeMillis() / 1000);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private static List<BigInteger> getXP(CurvePool pool, List<BigInteger> balances) {
        try {
            return (List<BigInteger>) MethodUtil.getNonAccessibleMethod(CurvePool.class,
                                                                        "getXP",
                                                                        List.class)
                                                .invoke(pool, balances);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    @Test
    public void getAmountTest() {
        Assertions.assertDoesNotThrow(() -> pool.init());
        IToken token0 = (IToken) pool.getTokens().get(0);
        IToken token1 = (IToken) pool.getTokens().get(1);
        List<BigInteger> balances = pool.getBalances();
        BigInteger A = pool.getA(System.currentTimeMillis() / 1000);
        BigInteger D0 = getD(pool, getXP(pool, balances), A);
        BigInteger amountIn = BigInteger.valueOf(10).pow(token0.getDecimals());
        BigInteger amountOut = pool.getAmountOut(((Contract) token0).getAddress(),
                                                 ((Contract) token1).getAddress(),
                                                 amountIn);
        log.info("{} {} -> {} {}", amountIn, token0.getSymbol(), amountOut, token1.getSymbol());

        balances.set(0, balances.get(0).add(amountIn));
        balances.set(1, balances.get(1).subtract(amountOut));
        BigInteger D1 = getD(pool, getXP(pool, balances), A);
        log.info("D {} -> {}", D0, D1);

        BigInteger precision = BigInteger.valueOf(10)
                                         .pow(Math.min(token0.getDecimals(), token1.getDecimals()));
        BigInteger diffRate = D1.subtract(D0).abs().multiply(precision).divide(D0);
        Assertions.assertEquals(0, diffRate.intValue());
    }

    @Test
    public void getApproximateFeeTest() {
        Assertions.assertDoesNotThrow(() -> pool.init());
        IToken token0 = (IToken) pool.getTokens().get(0);
        IToken token1 = (IToken) pool.getTokens().get(1);
        BigInteger amountIn = BigInteger.valueOf(10).pow(token0.getDecimals());
        BigInteger expect = amountIn.multiply(BigInteger.valueOf(4))
                                    .divide(BigInteger.valueOf(10000));
        Assertions.assertEquals(expect, pool.getApproximateFee(token0, token1, amountIn));

        amountIn = BigInteger.valueOf(10).pow(token1.getDecimals());
        expect = amountIn.multiply(BigInteger.valueOf(4)).divide(BigInteger.valueOf(10000));
        Assertions.assertEquals(expect, pool.getApproximateFee(token1, token0, amountIn));
    }

    @Test
    public void getPriceTest() {
        Assertions.assertDoesNotThrow(() -> pool.init());
        IToken token0 = (IToken) pool.getTokens().get(0);
        IToken token1 = (IToken) pool.getTokens().get(1);
        BigInteger one = BigInteger.valueOf(10).pow(token0.getDecimals());

        BigInteger expectPrice = Pool.PRICE_FACTOR.multiply(one).divide(getDeltaY(pool, 0, 1, one));
        Assertions.assertEquals(expectPrice, pool.getPrice(token0, token1));
    }

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
