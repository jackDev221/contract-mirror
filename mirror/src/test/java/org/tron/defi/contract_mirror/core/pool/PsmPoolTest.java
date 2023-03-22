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

import java.math.BigInteger;

import static org.tron.defi.contract_mirror.core.ContractType.PSM_POOL;

@Slf4j
@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = TestApplication.class)
public class PsmPoolTest {
    @Autowired
    private ContractConfigList contractConfigList;
    @Autowired
    private ContractManager contractManager;
    private PsmPool pool;

    @Test
    public void getAmountTest() {
        Assertions.assertDoesNotThrow(() -> pool.init());
        IToken token0 = (IToken) pool.getTokens().get(0);
        IToken token1 = (IToken) pool.getTokens().get(1);
        BigInteger amountIn = BigInteger.valueOf(10).pow(token1.getDecimals());
        BigInteger expectOut = BigInteger.valueOf(10).pow(token0.getDecimals());
        Assertions.assertTrue(token0.balanceOf(pool.getAddress()).compareTo(expectOut) >= 0);
        BigInteger amountOut = pool.getAmountOut(((Contract) token1).getAddress(),
                                                 ((Contract) token0).getAddress(),
                                                 amountIn);
        Assertions.assertEquals(expectOut, amountOut);
    }

    @Test
    public void getApproximateFeeTest() {
        Assertions.assertDoesNotThrow(() -> pool.init());
        IToken token0 = (IToken) pool.getTokens().get(0);
        IToken token1 = (IToken) pool.getTokens().get(1);

        BigInteger amountIn = BigInteger.valueOf(10).pow(token0.getDecimals());
        BigInteger fee = pool.getApproximateFee(token0, token1, amountIn);
        Assertions.assertEquals(0, fee.intValue());

        amountIn = BigInteger.valueOf(10).pow(token1.getDecimals());
        fee = pool.getApproximateFee(token1, token0, amountIn);
        Assertions.assertEquals(0, fee.intValue());
    }

    @Test
    public void getPriceTest() {
        Assertions.assertDoesNotThrow(() -> pool.init());
        IToken token0 = (IToken) pool.getTokens().get(0);
        IToken token1 = (IToken) pool.getTokens().get(1);
        Assertions.assertEquals(Pool.PRICE_FACTOR.multiply(pool.getGemToUsddDecimalFactor()),
                                pool.getPrice(token0, token1));
        Assertions.assertEquals(Pool.PRICE_FACTOR.divide(pool.getGemToUsddDecimalFactor()),
                                pool.getPrice(token1, token0));
    }

    @BeforeEach
    public void setUp() {
        ContractConfigList.ContractConfig config = null;
        for (ContractConfigList.ContractConfig contractConfig : contractConfigList.getContracts()) {
            if (PSM_POOL == contractConfig.getType()) {
                config = contractConfig;
                break;
            }
        }
        Assertions.assertNotNull(config);
        pool = (PsmPool) contractManager.registerContract(new PsmPool(config.getAddress(),
                                                                      config.getPolyAddress()));
    }

    @Test
    public void initTest() {
        Assertions.assertDoesNotThrow(() -> pool.init());
        log.info(pool.info());
    }
}
