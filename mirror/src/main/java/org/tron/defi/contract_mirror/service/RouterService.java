package org.tron.defi.contract_mirror.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.tron.defi.contract_mirror.config.RouterConfig;
import org.tron.defi.contract_mirror.core.graph.Edge;
import org.tron.defi.contract_mirror.core.graph.Graph;
import org.tron.defi.contract_mirror.core.pool.Pool;
import org.tron.defi.contract_mirror.core.token.IToken;
import org.tron.defi.contract_mirror.dao.RouterPath;
import org.tron.defi.contract_mirror.strategy.StrategyFactory;
import org.tron.defi.contract_mirror.utils.TokenMath;

import java.math.BigInteger;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
public class RouterService {
    @Autowired
    private Graph graph;
    @Autowired
    private RouterConfig routerConfig;

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
        BigInteger denominator = path.getAmountIn().multiply(Pool.PRICE_FACTOR)
                                     .divide(path.getAmountOut());
        path.setImpact(numerator.negate().multiply(Pool.PRICE_FACTOR).divide(denominator));
        return path;
    }

    public List<RouterPath> getPath(String from, String to, BigInteger amountIn) {
        return StrategyFactory.getInstance()
                              .getStrategy(graph, routerConfig)
                              .getPath(from, to, amountIn)
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
        return StrategyFactory.getInstance()
                              .getStrategy(graph, config)
                              .getPath(from, to, amountIn)
                              .stream()
                              .map(RouterService::calculateFee)
                              .map(RouterService::calculateImpact)
                              .collect(Collectors.toList());
    }
}
