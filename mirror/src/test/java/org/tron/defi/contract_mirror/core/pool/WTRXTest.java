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
import org.tron.defi.contract_mirror.utils.TokenMath;

import java.math.BigInteger;

import static org.tron.defi.contract_mirror.core.ContractType.WTRX_TOKEN;

@Slf4j
@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = TestApplication.class)
public class WTRXTest {
    @Autowired
    private ContractConfigList contractConfigList;
    @Autowired
    private ContractManager contractManager;
    private WTRX wtrx;

    @Test
    public void getAmountOutTest() {
        Assertions.assertDoesNotThrow(() -> wtrx.init());
        IToken token0 = (IToken) wtrx.getTokens().get(0);
        IToken token1 = (IToken) wtrx.getTokens().get(1);
        BigInteger amountIn = BigInteger.valueOf(10).pow(token0.getDecimals());
        BigInteger expectOut = BigInteger.valueOf(10).pow(token1.getDecimals());
        BigInteger amountOut = wtrx.getAmountOut(((Contract) token0).getAddress(),
                                                 ((Contract) token1).getAddress(),
                                                 amountIn);
        Assertions.assertEquals(expectOut, amountOut);

        BigInteger maxOut = token1.balanceOf(wtrx.getAddress());
        Assertions.assertThrows(RuntimeException.class,
                                () -> wtrx.getAmountOut(((Contract) token0).getAddress(),
                                                        ((Contract) token1).getAddress(),
                                                        TokenMath.safeAdd(maxOut, BigInteger.ONE)),
                                "NOT ENOUGH BALANCE");
    }

    @Test
    public void getApproximateFeeTest() {
        Assertions.assertDoesNotThrow(() -> wtrx.init());
        IToken token0 = (IToken) wtrx.getTokens().get(0);
        IToken token1 = (IToken) wtrx.getTokens().get(1);
        Assertions.assertEquals(BigInteger.ZERO,
                                wtrx.getApproximateFee(token0, token1, BigInteger.ONE));
        Assertions.assertEquals(BigInteger.ZERO,
                                wtrx.getApproximateFee(token1, token0, BigInteger.ONE));
    }

    @Test
    public void getPriceTest() {
        Assertions.assertDoesNotThrow(() -> wtrx.init());
        IToken token0 = (IToken) wtrx.getTokens().get(0);
        IToken token1 = (IToken) wtrx.getTokens().get(1);
        Assertions.assertEquals(Pool.PRICE_FACTOR, wtrx.getPrice(token0, token1));
        Assertions.assertEquals(Pool.PRICE_FACTOR, wtrx.getPrice(token1, token0));
    }

    @Test
    public void initTest() {
        Assertions.assertNotNull(wtrx);
        Assertions.assertDoesNotThrow(() -> wtrx.init());
        Assertions.assertEquals(0, wtrx.cost());
        Assertions.assertEquals("WTRX", wtrx.getName());
    }

    @BeforeEach
    public void setUp() {
        contractManager.initTRX();
        for (ContractConfigList.ContractConfig config : contractConfigList.getContracts()) {
            if (WTRX_TOKEN == config.getType()) {
                log.info(config.toString());
                wtrx = (WTRX) contractManager.registerContract(new WTRX(config.getAddress()));
            }
        }
    }
}
