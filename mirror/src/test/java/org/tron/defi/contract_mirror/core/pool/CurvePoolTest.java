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
import org.tron.defi.contract_mirror.utils.FieldUtil;
import org.tron.defi.contract_mirror.utils.MethodUtil;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.List;

import static org.tron.defi.contract_mirror.core.ContractType.CURVE_POOL;

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
    public void calcWithdrawOneCoinTest() throws IllegalAccessException {
        Assertions.assertDoesNotThrow(() -> pool.init());
        // mock fee
        FieldUtil.set(pool, "fee", new BigInteger("4000000"));
        FieldUtil.set(pool, "adminFee", new BigInteger("5000000000"));
        // mock A
        FieldUtil.set(pool, "initialA", new BigInteger("100"));
        FieldUtil.set(pool, "futureA", new BigInteger("100"));
        FieldUtil.set(pool, "timeInitialA", 0L);
        FieldUtil.set(pool, "timeFutureA", 0L);
        // mock balances
        List<BigInteger> balances = Arrays.asList(new BigInteger("213980610348072217030877"),
                                                  new BigInteger("1813871592775292307512216"),
                                                  new BigInteger("1364554176737"));
        FieldUtil.set(pool, "balances", balances);
        pool.getLpToken().setTotalSupply(new BigInteger("3338872032932939434862503"));
        BigInteger amountIn = new BigInteger("973235011826209399");
        BigInteger expectOut = new BigInteger("889466862886478962");
        BigInteger amountOut = pool.calcWithdrawOneCoin(amountIn,
                                                        0,
                                                        System.currentTimeMillis() / 1000)
                                   .getFirst();
        Assertions.assertEquals(expectOut, amountOut);
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
    public void getAmountTest() throws IllegalAccessException {
        Assertions.assertDoesNotThrow(() -> pool.init());
        // mock fee
        FieldUtil.set(pool, "fee", new BigInteger("4000000"));
        FieldUtil.set(pool, "adminFee", new BigInteger("5000000000"));
        // mock A
        FieldUtil.set(pool, "initialA", new BigInteger("100"));
        FieldUtil.set(pool, "futureA", new BigInteger("100"));
        FieldUtil.set(pool, "timeInitialA", 0L);
        FieldUtil.set(pool, "timeFutureA", 0L);
        // mock balances
        List<BigInteger> balances = Arrays.asList(new BigInteger("213980610348072217030877"),
                                                  new BigInteger("1766750074472595543943163"),
                                                  new BigInteger("1411460447587"));
        FieldUtil.set(pool, "balances", balances);

        IToken token0 = (IToken) pool.getTokens().get(0);
        IToken token1 = (IToken) pool.getTokens().get(1);
        BigInteger amountIn = BigInteger.valueOf(10).pow(token0.getDecimals());
        BigInteger expectOut = new BigInteger("1120571402058473082");
        BigInteger amountOut = pool.getAmountOut(((Contract) token0).getAddress(),
                                                 ((Contract) token1).getAddress(),
                                                 amountIn);
        Assertions.assertEquals(expectOut, amountOut);
        log.info("{} {} -> {} {}", amountIn, token0.getSymbol(), amountOut, token1.getSymbol());
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
            if (CURVE_POOL == contractConfig.getType() &&
                contractConfig.getName().equals("old3pool")) {
                config = contractConfig;
                break;
            }
        }
        Assertions.assertNotNull(config);
        log.info(config.toString());
        pool = (CurvePool) contractManager.registerContract(new CurvePool(config.getAddress(),
                                                                          PoolType.convertFromContractType(
                                                                              config.getType()),
                                                                          config.getCurveConfig()));
    }
}
