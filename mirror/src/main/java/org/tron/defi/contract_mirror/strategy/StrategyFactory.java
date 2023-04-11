package org.tron.defi.contract_mirror.strategy;

import lombok.Getter;
import org.tron.defi.contract_mirror.config.RouterConfig;
import org.tron.defi.contract_mirror.core.graph.Graph;

import java.util.Arrays;
import java.util.List;

public class StrategyFactory {
    @Getter
    private static final StrategyFactory instance = new StrategyFactory();
    @Getter
    private List<String> strategyNames = Arrays.asList("DEFAULT", "OPTIMISTIC");

    private StrategyFactory() {
    }

    public IStrategy getStrategy(Graph graph, RouterConfig config) {
        switch (config.getStrategy()) {
            case "OPTIMISTIC":
                return new OptimisticStrategy(graph, config);
            default:
                return new DefaultStrategy(graph, config);
        }
    }
}
