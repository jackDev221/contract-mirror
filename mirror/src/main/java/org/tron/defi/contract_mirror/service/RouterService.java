package org.tron.defi.contract_mirror.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.tron.defi.contract_mirror.config.RouterConfig;
import org.tron.defi.contract_mirror.core.graph.Graph;
import org.tron.defi.contract_mirror.dao.RouterPath;
import org.tron.defi.contract_mirror.strategy.StrategyFactory;

import java.math.BigInteger;
import java.util.List;
import java.util.Set;

@Slf4j
@Service
public class RouterService {
    @Autowired
    private Graph graph;
    @Autowired
    private RouterConfig routerConfig;

    public List<RouterPath> getPath(String from, String to, BigInteger amountIn) {
        return StrategyFactory.getInstance()
                              .getStrategy(graph, routerConfig)
                              .getPath(from, to, amountIn);
    }

    public List<RouterPath> getPath(String from,
                                    String to,
                                    BigInteger amountIn,
                                    String strategy,
                                    int maxCost,
                                    int topN,
                                    Set<String> tokenWhiteList,
                                    Set<String> poolBlackList) {
        RouterConfig config = new RouterConfig();
        config.setStrategy(strategy);
        config.setMaxCost(maxCost);
        config.setTopN(topN);
        config.setTokenWhiteList(tokenWhiteList);
        config.setPoolBlackList(poolBlackList);
        return StrategyFactory.getInstance().getStrategy(graph, config).getPath(from, to, amountIn);
    }
}
