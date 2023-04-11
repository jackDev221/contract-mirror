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
import org.tron.defi.contract_mirror.utils.FieldUtil;
import org.tron.defi.contract_mirror.utils.MethodUtil;

import java.math.BigInteger;
import java.util.Arrays;
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

    @Test
    public void getAmountOutTest() throws IllegalAccessException {
        Assertions.assertDoesNotThrow(() -> pool.init());
        // mock fee
        FieldUtil.set(pool, "fee", new BigInteger("4000000"));
        FieldUtil.set(pool, "adminFee", new BigInteger("5000000000"));
        // mock A
        FieldUtil.set(pool, "initialA", new BigInteger("10000"));
        FieldUtil.set(pool, "futureA", new BigInteger("10000"));
        FieldUtil.set(pool, "timeInitialA", 0L);
        FieldUtil.set(pool, "timeFutureA", 0L);
        // mock base virtual price
        FieldUtil.set(pool, "virtualPrice", new BigInteger("1010341973595175236"));
        FieldUtil.set(pool, "timeUpdateVirtualPrice", System.currentTimeMillis() / 1000);
        // mock balances
        List<BigInteger> balances = Arrays.asList(new BigInteger("762224222984"),
                                                  new BigInteger("259724552463956158569454"));
        FieldUtil.set(pool, "balances", balances);

        IToken token0 = (IToken) pool.getTokens().get(0);
        IToken token1 = (IToken) pool.getUnderlyingPool().getLpToken();
        BigInteger amountIn = BigInteger.valueOf(10).pow(token0.getDecimals());
        BigInteger expectOut = new BigInteger("973235011826209399");
        BigInteger amountOut = pool.getAmountOut(((Contract) token0).getAddress(),
                                                 ((Contract) token1).getAddress(),
                                                 amountIn);
        log.info("{} {} -> {} {}", amountIn, token0.getSymbol(), amountOut, token1.getSymbol());
        Assertions.assertEquals(expectOut, amountOut);
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
    public void getAmountOutUnderlyingWithToken0Test() throws IllegalAccessException {
        Assertions.assertDoesNotThrow(() -> pool.init());
        // mock fee
        FieldUtil.set(pool, "fee", new BigInteger("4000000"));
        FieldUtil.set(pool, "adminFee", new BigInteger("5000000000"));
        // mock A
        FieldUtil.set(pool, "initialA", new BigInteger("10000"));
        FieldUtil.set(pool, "futureA", new BigInteger("10000"));
        FieldUtil.set(pool, "timeInitialA", 0L);
        FieldUtil.set(pool, "timeFutureA", 0L);
        // mock base virtual price
        FieldUtil.set(pool, "virtualPrice", new BigInteger("1010362674886673624"));
        FieldUtil.set(pool, "timeUpdateVirtualPrice", System.currentTimeMillis() / 1000);
        // mock balances
        List<BigInteger> balances = Arrays.asList(new BigInteger("762224222984"),
                                                  new BigInteger("259724552463956158569454"));
        FieldUtil.set(pool, "balances", balances);
        // mock underlying pool
        balances = Arrays.asList(new BigInteger("213980610348072217030877"),
                                 new BigInteger("1766750074472595543943163"),
                                 new BigInteger("1411460447587"));
        FieldUtil.set(pool.getUnderlyingPool(), "balances", balances);
        pool.getUnderlyingPool()
            .getLpToken()
            .setTotalSupply(new BigInteger("3338872032932939434862503"));

        IToken token0 = (IToken) pool.getTokens().get(0);
        IToken token1 = (IToken) pool.getTokens().get(1);

        BigInteger amountIn = BigInteger.valueOf(10).pow(token0.getDecimals());
        BigInteger expectOut = new BigInteger("890040532631141956");
        BigInteger amountOut = pool.getAmountOut(((Contract) token0).getAddress(),
                                                 ((Contract) token1).getAddress(),
                                                 amountIn);
        Assertions.assertEquals(expectOut, amountOut);

        amountIn = BigInteger.valueOf(10).pow(token1.getDecimals());
        expectOut = new BigInteger("1122018");
        amountOut = pool.getAmountOut(((Contract) token1).getAddress(),
                                      ((Contract) token0).getAddress(),
                                      amountIn);
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
            PoolType.CURVE_COMBINATION4,
            config.getCurveConfig()));
    }
}
