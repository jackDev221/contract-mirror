package org.tron.defi.contract_mirror.service;

import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.tron.defi.contract_mirror.config.RouterConfig;
import org.tron.defi.contract_mirror.core.graph.Edge;
import org.tron.defi.contract_mirror.core.graph.Graph;
import org.tron.defi.contract_mirror.core.pool.Pool;
import org.tron.defi.contract_mirror.core.token.IToken;
import org.tron.defi.contract_mirror.dao.RouterInfo;
import org.tron.defi.contract_mirror.dao.RouterPath;
import org.tron.defi.contract_mirror.strategy.StrategyFactory;
import org.tron.defi.contract_mirror.utils.TokenMath;

import java.math.BigInteger;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
public class RouterService {
    private final DistributionSummary candidateNum;
    private final Map<String, DistributionSummary> strategyDuration = new HashMap<>();
    @Autowired
    private Graph graph;
    @Autowired
    private RouterConfig routerConfig;

    @Autowired
    public RouterService(MeterRegistry meterRegistry) {
        candidateNum = DistributionSummary.builder("num_router_candidates")
                                          .description("Number of path candidates")
                                          .publishPercentileHistogram()
                                          .publishPercentiles(0.5, 0.8, 0.99)
                                          .percentilePrecision(3)
                                          .register(meterRegistry);
        for (String strategy : StrategyFactory.getInstance().getStrategyNames()) {
            strategyDuration.put(strategy,
                                 DistributionSummary.builder("strategy_duration_" + strategy)
                                                    .description("Time token for strategy")
                                                    .baseUnit("ms")
                                                    .publishPercentileHistogram()
                                                    .publishPercentiles(0.5, 0.95, 0.99, 0.9999)
                                                    .percentilePrecision(5)
                                                    .register(meterRegistry));
        }
    }

    private static RouterPath calculateFee(RouterPath path) {
        path.setFee(BigInteger.ZERO);
        BigInteger amountIn = path.getAmountIn();
        for (RouterPath.Step step : path.getSteps()) {
            Edge edge = step.getEdge();
            BigInteger poolFee = edge.getPool()
                                     .getApproximateFee((IToken) edge.getFrom().getToken(),
                                                        (IToken) edge.getTo().getToken(),
                                                        amountIn);
            path.setFee(TokenMath.safeAdd(path.getFee(), poolFee));
            amountIn = TokenMath.safeSubtract(amountIn, poolFee);
        }
        return path;
    }

    private static RouterPath calculateImpact(RouterPath path) {
        /*
          $actualPrice = amountIn / amountOut * \prod{1-FEE/FEE_DENOMINATOR} -
          \prod{reserveIn/reserveOut}$
          which is $actualPrice = (amountIn - fee) / amountOut$
          $price = \prod pricePool$
          $numerator = |actualPrice - price|$
          $denominator = |amountIn / amountOut|$
         */
        BigInteger denominator = path.getAmountIn()
                                     .multiply(Pool.PRICE_FACTOR)
                                     .divide(path.getAmountOut());
        if (denominator.compareTo(BigInteger.ZERO) == 0) {
            // impact is approximately equal to 0
            path.setImpact(BigInteger.ZERO);
            return path;
        }
        BigInteger price = Pool.PRICE_FACTOR;
        for (RouterPath.Step step : path.getSteps()) {
            Edge edge = step.getEdge();
            price = price.multiply(edge.getPool()
                                       .getPrice((IToken) edge.getFrom().getToken(),
                                                 (IToken) edge.getTo().getToken()))
                         .divide(Pool.PRICE_FACTOR);
        }
        BigInteger numerator = path.getAmountIn()
                                   .subtract(path.getFee())
                                   .multiply(Pool.PRICE_FACTOR)
                                   .divide(path.getAmountOut())
                                   .subtract(price)
                                   .abs();
        path.setImpact(numerator.negate().multiply(Pool.PRICE_FACTOR).divide(denominator));
        return path;
    }

    public List<RouterPath> getPath(String from, String to, BigInteger amountIn) {
        DistributionSummary duration = strategyDuration.getOrDefault(routerConfig.getStrategy(),
                                                                     strategyDuration.get(
                                                                         "DEFAULT"));
        long t0 = System.currentTimeMillis();
        RouterInfo routerInfo = StrategyFactory.getInstance()
                                               .getStrategy(graph, routerConfig)
                                               .getPath(from, to, amountIn);
        long t1 = System.currentTimeMillis();
        duration.record(t1 - t0);
        candidateNum.record(routerInfo.getTotalCandidates());
        return routerInfo.getPaths()
                         .stream()
                         .map(RouterService::calculateFee)
                         .map(RouterService::calculateImpact)
                         .collect(Collectors.toList());
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
        DistributionSummary duration = strategyDuration.getOrDefault(routerConfig.getStrategy(),
                                                                     strategyDuration.get(
                                                                         "DEFAULT"));
        long t0 = System.currentTimeMillis();
        RouterInfo routerInfo = StrategyFactory.getInstance()
                                               .getStrategy(graph, config)
                                               .getPath(from, to, amountIn);
        long t1 = System.currentTimeMillis();
        duration.record(t1 - t0);
        candidateNum.record(routerInfo.getTotalCandidates());
        return routerInfo.getPaths()
                         .stream()
                         .map(RouterService::calculateFee)
                         .map(RouterService::calculateImpact)
                         .collect(Collectors.toList());
    }
}
