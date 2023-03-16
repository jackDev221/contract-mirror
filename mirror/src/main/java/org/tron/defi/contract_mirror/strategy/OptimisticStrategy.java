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
public class OptimisticStrategy extends DefaultStrategy implements IStrategy {
    public OptimisticStrategy(Graph graph, RouterConfig routerConfig) {
        super(graph, routerConfig);
    }

    @Override
    public List<RouterPath> getPath(String from, String to, BigInteger amountIn) {
        Node nodeFrom = graph.getNode(from);
        Node nodeTo = graph.getNode(to);
        if (null == nodeFrom || null == nodeTo) {
            throw new IllegalArgumentException("INVALID FROM/TO ADDRESS");
        }
        long time0 = System.currentTimeMillis();
        PriorityQueue<RouterPath> minHeap = new PriorityQueue<>(routerConfig.getTopN(),
                                                                Comparator.comparing(RouterPath::getAmountOut));
        Map<Node, Pair<Integer, RouterPath>> bestPaths = new HashMap<>(30000);
        RouterPath initialPath = new RouterPath(nodeFrom, amountIn, nodeTo);
        bestPaths.put(nodeFrom, Pair.of(0, initialPath));
        // BFS
        Queue<RouterPath> searchPaths = new LinkedList<>();
        searchPaths.offer(initialPath);
        int stepCount = 0;
        int candidateNum = 0;
        while (!searchPaths.isEmpty()) {
            int n = searchPaths.size();
            while (n-- > 0) {
                RouterPath currentPath = searchPaths.poll();
                RouterPath.Step currentStep = currentPath.getCurrentStep();
                BigInteger amountInStep = null == currentStep
                                          ? currentPath.getAmountIn()
                                          : currentStep.getAmountOut();
                if (null == amountInStep || 0 == amountInStep.compareTo(BigInteger.ZERO)) {
                    // branch has been cut
                    continue;
                }
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
                    BigInteger amountOutStep;
                    try {
                        amountOutStep = edge.getPool()
                                            .getAmountOut(edge.getFrom().getToken().getAddress(),
                                                          edge.getTo().getToken().getAddress(),
                                                          amountInStep);
                    } catch (RuntimeException e) {
                        log.debug("ERROR: {}", e.getMessage());
                        log.debug("CUT BRANCH {} |-> {}",
                                  getLogPath(currentPath),
                                  edge.getPool().getName());
                        continue;
                    }
                    if (found) {
                        RouterPath candidate = new RouterPath(currentPath);
                        candidate.addStep(edge);
                        candidate.setAmountOut(amountOutStep);
                        minHeap.offer(candidate);
                        candidateNum++;
                        log.debug("NEW CANDIDATE {} {}", amountOutStep, getLogPath(candidate));
                        if (minHeap.size() > routerConfig.getTopN()) {
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
                    if (null != bestAmount && bestAmount.compareTo(amountOutStep) >= 0) {
                        log.debug("CUT BRANCH {} |-> {}",
                                  getLogPath(currentPath),
                                  edge.getPool().getName());
                        continue;
                    }
                    RouterPath path = new RouterPath(currentPath);
                    path.addStep(edge);
                    path.getCurrentStep().setAmountOut(amountOutStep);
                    bestPaths.put(edge.getTo(), Pair.of(stepCount, path));
                    searchPaths.offer(path);
                    if (null != bestInfo && bestInfo.getFirst() >= stepCount) {
                        RouterPath pathToCut = bestInfo.getSecond();
                        pathToCut.setAmountOut(BigInteger.ZERO);
                        log.debug("CUT BRANCH {} |-> {}",
                                  getLogPath(currentPath),
                                  edge.getPool().getName());
                    }
                }
            }
            stepCount++;
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
        log.info("Get top {} of {} paths in {}ms", path.length, candidateNum, time1 - time0);
        return List.of(path);
    }
}
