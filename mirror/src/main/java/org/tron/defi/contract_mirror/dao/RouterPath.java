package org.tron.defi.contract_mirror.dao;

import lombok.Data;
import org.tron.defi.contract_mirror.core.graph.Edge;
import org.tron.defi.contract_mirror.core.graph.Node;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Data
public class RouterPath {
    private final Node from;
    private final BigInteger amountIn;
    private final Node to;
    private BigInteger amountOut;
    private BigInteger fee;
    private BigInteger impact;
    private List<Step> steps = new ArrayList<>();
    private Set<Node> nodes = new HashSet<>();
    private int cost = 0;

    public RouterPath(RouterPath path) {
        from = path.getFrom();
        amountIn = path.getAmountIn();
        to = path.getTo();
        steps = new ArrayList<>(path.getSteps());
        cost = path.getCost();
    }

    public RouterPath(Node from, BigInteger amountIn, Node to) {
        this.from = from;
        this.amountIn = amountIn;
        this.to = to;
    }

    public void addStep(Edge edge) {
        if (isBackward(edge)) {
            return;
        }
        nodes.add(edge.getTo());
        Step nextStep = new Step();
        nextStep.setEdge(edge);
        steps.add(nextStep);
        cost += edge.getPool().cost();
    }

    public Step getCurrentStep() {
        return steps.isEmpty() ? null : steps.get(steps.size() - 1);
    }

    public boolean isBackward(Edge edge) {
        // It's not reasonable step to same pool of current step or an old node
        Step currentStep = getCurrentStep();
        return nodes.contains(edge.getTo()) ||
               (null != currentStep && currentStep.getEdge().getPool().equals(edge.getPool()));
    }

    @Data
    public static class Step {
        private BigInteger amountIn;
        private BigInteger amountOut;
        private Edge edge;
    }
}
