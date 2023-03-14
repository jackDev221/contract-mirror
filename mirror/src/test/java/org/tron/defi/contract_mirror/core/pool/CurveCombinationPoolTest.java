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
import org.tron.defi.contract_mirror.core.Contract;
import org.tron.defi.contract_mirror.core.ContractManager;
import org.tron.defi.contract_mirror.core.token.IToken;
import org.tron.defi.contract_mirror.utils.MethodUtil;

import java.math.BigInteger;
import java.util.List;

import static org.tron.defi.contract_mirror.core.ContractType.CURVE_3POOL;
import static org.tron.defi.contract_mirror.core.ContractType.CURVE_COMBINATION_4POOL;

@Slf4j
@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = TestApplication.class)
public class CurveCombinationPoolTest {
    @Autowired
    private ContractConfigList contractConfigList;
    @Autowired
    private ContractManager contractManager;
    private ContractConfigList.ContractConfig config;
    private CurveCombinationPool pool;

    private static BigInteger getD(CurveCombinationPool pool, List<BigInteger> xp, BigInteger A) {
        try {
            return (BigInteger) MethodUtil.getNonAccessibleMethod(CurveCombinationPool.class,
                                                                  "getD",
                                                                  List.class,
                                                                  BigInteger.class)
                                          .invoke(pool, xp, A);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private static BigInteger getDeltaY(CurveCombinationPool pool,
                                        int i,
                                        int j,
                                        BigInteger amountIn) {
        try {
            return (BigInteger) MethodUtil.getNonAccessibleMethod(CurveCombinationPool.class,
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

    private static BigInteger getDeltaYUnderying(CurveCombinationPool pool,
                                                 int i,
                                                 int j,
                                                 BigInteger amountIn) {
        try {
            return (BigInteger) MethodUtil.getNonAccessibleMethod(CurveCombinationPool.class,
                                                                  "getDeltaYUnderlying",
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

    private static BigInteger getVirtualPrice(CurveCombinationPool pool) {
        try {
            return (BigInteger) MethodUtil.getNonAccessibleMethod(CurveCombinationPool.class,
                                                                  "getVirtualPrice",
                                                                  long.class,
                                                                  boolean.class)
                                          .invoke(pool, System.currentTimeMillis() / 1000, false);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private static List<BigInteger> getXP(CurveCombinationPool pool,
                                          List<BigInteger> balances,
                                          BigInteger price) {
        try {
            return (List<BigInteger>) MethodUtil.getNonAccessibleMethod(CurveCombinationPool.class,
                                                                        "getXP",
                                                                        List.class,
                                                                        BigInteger.class)
                                                .invoke(pool, balances, price);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    @Test
    public void getAmountOutTest() {
        Assertions.assertDoesNotThrow(() -> pool.init());
        IToken token0 = (IToken) pool.getTokens().get(0);
        IToken token1 = (IToken) pool.getUnderlyingPool().getLpToken();
        List<BigInteger> balances = pool.getBalances();
        BigInteger virtualPrice = getVirtualPrice(pool);
        BigInteger A = pool.getA(System.currentTimeMillis() / 1000);
        BigInteger D0 = getD(pool, getXP(pool, balances, virtualPrice), A);
        BigInteger amountIn = BigInteger.valueOf(10).pow(token0.getDecimals());
        BigInteger amountOut = pool.getAmountOut(((Contract) token0).getAddress(),
                                                 ((Contract) token1).getAddress(),
                                                 amountIn);
        log.info("{} {} -> {} {}", amountIn, token0.getSymbol(), amountOut, token1.getSymbol());

        balances.set(0, balances.get(0).add(amountIn));
        balances.set(1, balances.get(1).subtract(amountOut));
        BigInteger D1 = getD(pool, getXP(pool, balances, virtualPrice), A);
        log.info("D {} -> {}", D0, D1);

        BigInteger precision = BigInteger.valueOf(10)
                                         .pow(Math.min(token0.getDecimals(), token1.getDecimals()));
        BigInteger diffRate = D1.subtract(D0).abs().multiply(precision).divide(D0);
        Assertions.assertEquals(0, diffRate.intValue());
    }

    @Test
    public void getAmountOutUnderlyingTest() {
        Assertions.assertDoesNotThrow(() -> pool.init());
        IToken token0 = (IToken) pool.getTokens().get(1);
        IToken token1 = (IToken) pool.getTokens().get(2);
        BigInteger amountIn = BigInteger.valueOf(10).pow(token0.getDecimals());
        BigInteger expectOut = pool.getUnderlyingPool()
                                   .getAmountOut(((Contract) token0).getAddress(),
                                                 ((Contract) token1).getAddress(),
                                                 amountIn);
        BigInteger amountOut = pool.getAmountOut(((Contract) token0).getAddress(),
                                                 ((Contract) token1).getAddress(),
                                                 amountIn);
        log.info("{} {} -> {} {}", amountIn, token0.getSymbol(), amountOut, token1.getSymbol());
        Assertions.assertEquals(expectOut, amountOut);
    }

    @Test
    public void getApproximateFeeTest() {
        Assertions.assertDoesNotThrow(() -> pool.init());
        IToken token0 = (IToken) pool.getTokens().get(0);
        IToken token1 = (IToken) pool.getUnderlyingPool().getLpToken();
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
        IToken token1 = (IToken) pool.getUnderlyingPool().getLpToken();
        BigInteger one = BigInteger.valueOf(10).pow(token0.getDecimals());

        BigInteger expectPrice = Pool.PRICE_FACTOR.multiply(one).divide(getDeltaY(pool, 0, 1, one));
        Assertions.assertEquals(expectPrice, pool.getPrice(token0, token1));
    }

    @Test
    public void getPriceUnderlyingTest() {
        Assertions.assertDoesNotThrow(() -> pool.init());
        IToken token0 = (IToken) pool.getTokens().get(0);
        IToken token1 = (IToken) pool.getTokens().get(1);
        BigInteger one = BigInteger.valueOf(10).pow(token0.getDecimals());

        BigInteger expectPrice = Pool.PRICE_FACTOR.multiply(one)
                                                  .divide(getDeltaYUnderying(pool, 0, 1, one));
        Assertions.assertEquals(expectPrice, pool.getPrice(token0, token1));
    }

    @Test
    public void initTest() {
        Assertions.assertDoesNotThrow(() -> pool.init());
        log.info(pool.info());
    }

    @BeforeEach
    public void setUp() {
        for (ContractConfigList.ContractConfig contractConfig : contractConfigList.getContracts()) {
            if (CURVE_3POOL == contractConfig.getType()) {
                contractManager.initCurve(contractConfig);
            } else if (CURVE_COMBINATION_4POOL == contractConfig.getType()) {
                config = contractConfig;
                break;
            }
        }
        Assertions.assertNotNull(config);
        log.info(config.toString());
        pool = (CurveCombinationPool) contractManager.registerContract(new CurveCombinationPool(
            config.getAddress(),
            PoolType.CURVE_COMBINATION4));
    }
}
