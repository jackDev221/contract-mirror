package org.tron.defi.contract_mirror.service;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.tron.defi.contract_mirror.TestApplication;
import org.tron.defi.contract_mirror.config.PriceCenterConfig;
import org.tron.defi.contract_mirror.config.TokenConfigList;
import org.tron.defi.contract_mirror.core.Contract;
import org.tron.defi.contract_mirror.core.ContractManager;
import org.tron.defi.contract_mirror.core.token.IToken;
import org.tron.defi.contract_mirror.core.token.TRC20;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Slf4j
@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = TestApplication.class)
public class PriceServiceTest {
    @Autowired
    private TokenConfigList tokenConfigList;
    @Autowired
    private ContractManager contractManager;
    @Autowired
    private PriceCenterConfig priceCenterConfig;
    @Autowired
    private PriceService priceService;
    private IToken token;

    @Test
    public void cacheExpireTest() {
        String symbol = token.getSymbol();
        Assertions.assertNull(priceService.getPriceFromCache(symbol));
        BigDecimal price = priceService.getPrice(symbol);
        Assertions.assertEquals(price, priceService.getPriceFromCache(symbol));
        try {
            Thread.sleep(priceCenterConfig.getCacheConfig().getExpireTime() * 1000 + 100);
            Assertions.assertNull(priceService.getPriceFromCache(symbol));
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    public void getPriceByAddressTest() {
        String address = ((Contract) token).getAddress();
        Assertions.assertNull(priceService.getPriceFromCache(address));
        BigDecimal price = priceService.getPrice(address);
        Assertions.assertEquals(price, priceService.getPriceFromCache(address));
        log.info(price.toString());
        BigDecimal diffPercentage = price.subtract(BigDecimal.ONE)
                                         .abs()
                                         .divide(price, RoundingMode.FLOOR)
                                         .multiply(BigDecimal.valueOf(100));
        log.info(diffPercentage.toString());
        Assertions.assertTrue(diffPercentage.compareTo(BigDecimal.ONE) <= 1);
    }

    @Test
    public void getPriceBySymbolTest() {
        String symbol = token.getSymbol();
        Assertions.assertNull(priceService.getPriceFromCache(symbol));
        BigDecimal price = priceService.getPrice(symbol);
        Assertions.assertEquals(price, priceService.getPriceFromCache(symbol));
        log.info(price.toString());
        BigDecimal diffPercentage = price.subtract(BigDecimal.ONE)
                                         .abs()
                                         .divide(price, RoundingMode.FLOOR)
                                         .multiply(BigDecimal.valueOf(100));
        log.info(diffPercentage.toString());
        Assertions.assertTrue(diffPercentage.compareTo(BigDecimal.ONE) <= 1);
    }

    @BeforeEach
    public void setUp() {
        String address = tokenConfigList.getTokens().get("USDT");
        token = (IToken) contractManager.registerContract(new TRC20(address));
    }
}
