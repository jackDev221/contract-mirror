package org.tron.defi.contract_mirror.strategy;

import org.tron.defi.contract_mirror.config.RouterConfig;
import org.tron.defi.contract_mirror.core.graph.Edge;
import org.tron.defi.contract_mirror.core.graph.Graph;
import org.tron.defi.contract_mirror.core.graph.Node;
import org.tron.defi.contract_mirror.dao.RouterPath;

import java.math.BigInteger;
import java.util.*;

public class DefaultStrategy implements IStrategy {
    private final Graph graph;
    private final RouterConfig routerConfig;

    public DefaultStrategy(Graph graph, RouterConfig routerConfig) {
        this.graph = graph;
        this.routerConfig = routerConfig;
    }

    @Override
    public List<RouterPath> getPath(String from, String to, BigInteger amountIn) {
        Node nodeFrom = graph.getNode(from);
        Node nodeTo = graph.getNode(to);
        if (null == nodeFrom || null == nodeTo) {
            throw new IllegalArgumentException("INVALID FROM/TO ADDRESS");
        }
        List<RouterPath> candidates = new ArrayList<>();
        // BFS
        Queue<RouterPath> searchPaths = new LinkedList<>();
        searchPaths.offer(new RouterPath(nodeFrom, amountIn, nodeTo));
        int maxStep = 0;
        while (!searchPaths.isEmpty()) {
            int n = searchPaths.size();
            while (n-- > 0) {
                RouterPath currentPath = searchPaths.poll();
                RouterPath.Step currentStep = currentPath.getCurrentStep();
                Node node = null == currentStep
                            ? currentPath.getFrom()
                            : currentStep.getEdge().getTo();
                for (Edge edge : node.getOutEdges()) {
                    if (checkWhiteBlackList(edge) ||
                        currentPath.isBackward(edge) ||
                        edge.getPool().cost() + currentPath.getCost() > routerConfig.getMaxCost()) {
                        continue;
                    }
                    boolean found = edge.getTo().isEqual(currentPath.getTo());
                    RouterPath path = new RouterPath(currentPath);
                    path.addStep(edge);
                    if (!found) {
                        searchPaths.offer(path);
                    } else {
                        candidates.add(path);
                        maxStep = Math.max(maxStep, path.getSteps().size());
                    }
                }
            }
        }
        return getTopN(candidates, routerConfig.getTopN(), maxStep);
    }

    private boolean checkWhiteBlackList(Edge edge) {
        if (routerConfig.getPoolBlackList().contains(edge.getPool().getAddress())) {
            return false;
        }
        return routerConfig.getTokenWhiteList().isEmpty() ||
               routerConfig.getTokenWhiteList().contains(edge.getTo().getToken().getAddress());
    }

    private List<RouterPath> getTopN(List<RouterPath> candidates, int topN, int maxStep) {
        if (candidates.isEmpty()) {
            return candidates;
        }
        PriorityQueue<RouterPath> paths = new PriorityQueue<>(topN,
                                                              (path0, path1) -> path1.getAmountOut()
                                                                                     .compareTo(
                                                                                         path0.getAmountOut()));
        Map<Node, BigInteger> states = new HashMap<>();
        states.put(candidates.get(0).getFrom(), candidates.get(0).getAmountIn());
        for (int i = 0; i < maxStep; i++) {
            for (int j = 0; j < candidates.size(); j++) {
                RouterPath candidate = candidates.get(i);
                List<RouterPath.Step> steps = candidate.getSteps();
                if (i >= steps.size()) {
                    continue;
                }
                RouterPath.Step step = steps.get(i);
                step.setAmountIn(0 == i
                                 ? candidate.getAmountIn()
                                 : steps.get(i - 1).getAmountOut());
                if (0 == step.getAmountIn().compareTo(BigInteger.ZERO) ||
                    null != step.getAmountOut()) {
                    continue;
                }
                Edge edge = step.getEdge();
                BigInteger amountOut = edge.getPool()
                                           .getAmountOut(edge.getFrom().getToken().getAddress(),
                                                         edge.getTo().getToken().getAddress(),
                                                         step.getAmountIn());
                if (i == steps.size() - 1) {
                    candidate.setAmountOut(amountOut);
                    paths.add(candidate);
                    continue;
                }
                BigInteger bestAmount = states.getOrDefault(edge.getTo(), null);
                if (null == bestAmount || bestAmount.compareTo(amountOut) > 0) {
                    step.setAmountOut(amountOut);
                    states.put(edge.getTo(), amountOut);
                } else {
                    // cut branch
                    step.setAmountOut(BigInteger.ZERO);
                }
            }
        }
        return new ArrayList<>(paths);
    }
}
