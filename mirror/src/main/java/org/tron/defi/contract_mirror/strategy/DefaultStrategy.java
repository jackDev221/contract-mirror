package org.tron.defi.contract_mirror.strategy;

import lombok.extern.slf4j.Slf4j;
import org.springframework.data.util.Pair;
import org.tron.defi.contract_mirror.config.RouterConfig;
import org.tron.defi.contract_mirror.core.graph.Edge;
import org.tron.defi.contract_mirror.core.graph.Graph;
import org.tron.defi.contract_mirror.core.graph.Node;
import org.tron.defi.contract_mirror.dao.RouterPath;

import java.math.BigInteger;
import java.util.*;

@Slf4j
public class DefaultStrategy implements IStrategy {
    protected final Graph graph;
    protected final RouterConfig routerConfig;

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
        long time0 = System.currentTimeMillis();
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
                    boolean found = edge.getTo().isEqual(currentPath.getTo());
                    if (edge.getPool().cost() + currentPath.getCost() > routerConfig.getMaxCost() ||
                        edge.getTo().outDegree() <= 1 ||
                        currentPath.isBackward(edge) ||
                        (!found && !checkWhiteBlackList(edge))) {
                        continue;
                    }
                    RouterPath path = new RouterPath(currentPath);
                    path.addStep(edge);
                    if (!found) {
                        searchPaths.offer(path);
                    } else {
                        log.debug(getLogPath(path));
                        candidates.add(path);
                        maxStep = Math.max(maxStep, path.getSteps().size());
                    }
                }
            }
        }
        long time1 = System.currentTimeMillis();
        log.info("Get {} paths in {}ms, maxStep {}", candidates.size(), time1 - time0, maxStep);
        return getTopN(candidates, routerConfig.getTopN(), maxStep);
    }

    protected static String getLogPath(RouterPath path) {
        String out = "";
        for (int i = 0; i < path.getSteps().size(); i++) {
            out = out.concat(path.getSteps().get(i).getEdge().getPool().getName());
            if (i != path.getSteps().size() - 1) {
                out = out.concat(" -> ");
            }
        }
        return out;
    }

    private static List<RouterPath> getTopN(List<RouterPath> candidates, int topN, int maxStep) {
        if (candidates.isEmpty()) {
            return candidates;
        }
        long time0 = System.currentTimeMillis();
        PriorityQueue<RouterPath> minHeap = new PriorityQueue<>(topN,
                                                                Comparator.comparing(RouterPath::getAmountOut));
        Map<Node, Pair<Integer, RouterPath>> bestPaths = new HashMap<>();
        bestPaths.put(candidates.get(0).getFrom(), Pair.of(0, candidates.get(0)));
        for (int i = 0; i < maxStep; i++) {
            for (int j = 0; j < candidates.size(); j++) {
                RouterPath candidate = candidates.get(j);
                List<RouterPath.Step> steps = candidate.getSteps();
                if (i >= steps.size()) {
                    continue;
                }
                RouterPath.Step step = steps.get(i);
                step.setAmountIn(0 == i
                                 ? candidate.getAmountIn()
                                 : steps.get(i - 1).getAmountOut());
                if (null == step.getAmountIn() ||
                    0 == step.getAmountIn().compareTo(BigInteger.ZERO) ||
                    null != step.getAmountOut()) {
                    continue;
                }
                Edge edge = step.getEdge();
                BigInteger amountOut;
                try {
                    amountOut = edge.getPool()
                                    .getAmountOut(edge.getFrom().getToken().getAddress(),
                                                  edge.getTo().getToken().getAddress(),
                                                  step.getAmountIn());
                } catch (RuntimeException e) {
                    log.debug("ERROR: {}", e.getMessage());
                    prunePathAt(candidate, i);
                    continue;
                }
                if (i == steps.size() - 1) {
                    candidate.setAmountOut(amountOut);
                    minHeap.offer(candidate);
                    log.debug("NEW CANDIDATE {} {}", amountOut, getLogPath(candidate));
                    if (minHeap.size() > topN) {
                        candidate = minHeap.poll();
                        log.debug("OBSOLETE CANDIDATE {} {}",
                                  candidate.getAmountOut(),
                                  getLogPath(candidate));
                    }
                    continue;
                }
                Pair<Integer, RouterPath> bestInfo = bestPaths.getOrDefault(edge.getTo(), null);
                BigInteger bestAmount = null == bestInfo
                                        ? null
                                        : bestInfo.getSecond()
                                                  .getSteps()
                                                  .get(bestInfo.getFirst())
                                                  .getAmountOut();
                if (null == bestAmount || bestAmount.compareTo(amountOut) < 0) {
                    step.setAmountOut(amountOut);
                    bestPaths.put(edge.getTo(), Pair.of(i, candidate));
                    if (null != bestInfo && bestInfo.getFirst() >= i) {
                        prunePathAt(bestInfo.getSecond(), bestInfo.getFirst());
                    }
                } else {
                    prunePathAt(candidate, i);
                }
            }
        }
        // to get result in order
        int n = minHeap.size();
        if (0 == n) {
            return Collections.emptyList();
        }
        RouterPath[] path = new RouterPath[n];
        while (n-- > 0) {
            path[n] = minHeap.poll();
        }
        long time1 = System.currentTimeMillis();
        log.info("Get top {} from {} paths in {}ms", topN, candidates.size(), time1 - time0);
        return List.of(path);
    }

    private static void prunePathAt(RouterPath pathToPrune, int pos) {
        for (int j = pos; j < pathToPrune.getSteps().size(); j++) {
            RouterPath.Step step = pathToPrune.getSteps().get(j);
            if (null == step.getAmountIn() || 0 == step.getAmountIn().compareTo(BigInteger.ZERO)) {
                break;
            }
            step.setAmountOut(BigInteger.ZERO);
        }
        log.debug("Prune {} at pos {}", getLogPath(pathToPrune), pos);
    }

    protected boolean checkWhiteBlackList(Edge edge) {
        if (routerConfig.getPoolBlackList().contains(edge.getPool().getAddress())) {
            log.debug("{} is blocked by blacklist", edge.getPool().getAddress());
            return false;
        }
        boolean ok = routerConfig.getTokenWhiteList().isEmpty() ||
                     routerConfig.getTokenWhiteList()
                                 .contains(edge.getTo().getToken().getAddress());
        if (!ok) {
            log.debug("{} is blocked by whitelist", edge.getTo().getToken().getAddress());
        }
        return ok;
    }
}
