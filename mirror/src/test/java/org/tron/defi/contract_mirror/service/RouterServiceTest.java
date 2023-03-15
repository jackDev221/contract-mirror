package org.tron.defi.contract_mirror.service;

import com.alibaba.fastjson.JSONObject;
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
import org.tron.defi.contract_mirror.config.TokenConfigList;
import org.tron.defi.contract_mirror.core.ContractManager;
import org.tron.defi.contract_mirror.core.factory.SunswapV1Factory;
import org.tron.defi.contract_mirror.core.factory.SunswapV2Factory;
import org.tron.defi.contract_mirror.core.graph.Graph;
import org.tron.defi.contract_mirror.core.pool.SunswapV1Pool;
import org.tron.defi.contract_mirror.core.pool.SunswapV2Pool;
import org.tron.defi.contract_mirror.core.token.IToken;
import org.tron.defi.contract_mirror.dao.RouterPath;
import org.tron.defi.contract_mirror.utils.MethodUtil;

import java.math.BigInteger;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Slf4j
@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = TestApplication.class)
public class RouterServiceTest {
    @Autowired
    private ContractConfigList contractConfigList;
    @Autowired
    private TokenConfigList tokenConfigList;
    @Autowired
    private ContractManager contractManager;
    @Autowired
    private Graph graph;
    @Autowired
    private RouterService routerService;
    private String usdtAddress;
    private String usddAddress;

    @Test
    public void getPathTest() {
        Assertions.assertNotNull(usdtAddress);
        Assertions.assertNotNull(usddAddress);
        IToken usdt = (IToken) contractManager.getContract(usdtAddress);
        BigInteger amountIn = BigInteger.valueOf(10).pow(usdt.getDecimals());
        long time0 = System.currentTimeMillis();
        List<RouterPath> paths = routerService.getPath(usdtAddress, usddAddress, amountIn);
        long time1 = System.currentTimeMillis();
        Assertions.assertNotNull(paths);
        Assertions.assertFalse(paths.isEmpty());
        log.info("Time elapse {} ms, paths size {}", time1 - time0, paths.size());

        time0 = System.currentTimeMillis();
        paths = routerService.getPath(usdtAddress,
                                      usddAddress,
                                      amountIn,
                                      "DEFAULT",
                                      3,
                                      3,
                                      Collections.emptySet(),
                                      Collections.emptySet());
        time1 = System.currentTimeMillis();
        log.info("Time elapse {} ms, paths size {}", time1 - time0, paths.size());
        log.info(JSONObject.toJSONString(paths));
    }

    @BeforeEach
    public void setUp() {
        contractManager.initTRX();
        SunswapV1Factory v1Factory = null;
        SunswapV2Factory v2Factory = null;
        String wtrx = null;
        for (ContractConfigList.ContractConfig config : contractConfigList.getContracts()) {
            switch (config.getType()) {
                case SUNSWAP_FACTORY_V1:
                    v1Factory
                        = (SunswapV1Factory) contractManager.registerContract(new SunswapV1Factory(
                        config.getAddress()));
                    v1Factory.setGraph(graph);
                    break;
                case SUNSWAP_FACTORY_V2:
                    v2Factory
                        = (SunswapV2Factory) contractManager.registerContract(new SunswapV2Factory(
                        config.getAddress()));
                    v2Factory.setGraph(graph);
                    break;
                case WTRX_TOKEN:
                    wtrx = config.getAddress();
                    contractManager.initWTRX(wtrx);
                    break;
                case CURVE_2POOL:
                case CURVE_3POOL:
                case CURVE_COMBINATION_4POOL:
                    contractManager.initCurve(config);
                    break;
                default:
                    break;
            }
        }
        int count = 0;
        for (Map.Entry<String, String> config : tokenConfigList.getTokens().entrySet()) {
            switch (config.getKey()) {
                case "TRX":
                    continue;
                case "USDT":
                    usdtAddress = config.getValue();
                    break;
                case "USDD":
                    usddAddress = config.getValue();
                    break;
                default:
                    break;
            }
            SunswapV1Pool v1Pool = (SunswapV1Pool) v1Factory.getExchange(config.getValue());
            newExchange(v1Factory, config.getValue(), v1Pool.getAddress());
            SunswapV2Pool v2Pool = (SunswapV2Pool) v2Factory.getPairFromChain(wtrx,
                                                                              config.getValue());
            newPair(v2Factory, v2Pool.getAddress(), ++count);
        }
    }

    private void newExchange(SunswapV1Factory factory, String tokenAddress, String poolAddress) {
        try {
            MethodUtil.getNonAccessibleMethod(SunswapV1Factory.class,
                                              "newExchange",
                                              String.class,
                                              String.class)
                      .invoke(factory, tokenAddress, poolAddress);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void newPair(SunswapV2Factory factory, String poolAddress, int newLength) {
        try {
            MethodUtil.getNonAccessibleMethod(SunswapV2Factory.class,
                                              "newPair",
                                              String.class,
                                              int.class).invoke(factory, poolAddress, newLength);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
