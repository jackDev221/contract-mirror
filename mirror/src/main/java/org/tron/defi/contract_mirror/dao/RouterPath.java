package org.tron.defi.contract_mirror.dao;

import com.alibaba.fastjson.JSONObject;
import lombok.Data;
import org.tron.defi.contract_mirror.core.graph.Edge;
import org.tron.defi.contract_mirror.core.graph.Node;
import org.tron.defi.contract_mirror.core.token.IToken;

import java.math.BigInteger;
import java.util.*;
import java.util.stream.Collectors;

@Data
public class RouterPath {
    private final Node from;
    private final BigInteger amountIn;
    private final Node to;
    private final List<Step> steps;
    private final Set<Node> nodes;
    private BigInteger amountOut;
    private BigInteger fee;
    private BigInteger impact;
    private int cost = 0;

    public RouterPath(RouterPath path) {
        from = path.getFrom();
        amountIn = path.getAmountIn();
        to = path.getTo();
        steps = new ArrayList<>(path.getSteps());
        nodes = new HashSet<>(path.getNodes());
        cost = path.getCost();
    }

    public RouterPath(Node from, BigInteger amountIn, Node to) {
        this.from = from;
        this.amountIn = amountIn;
        this.to = to;
        steps = new ArrayList<>();
        nodes = new HashSet<>(Collections.singleton(from));
    }

    @Override
    public String toString() {
        JSONObject object = new JSONObject();
        object.put("from", from.getToken().getInfo());
        object.put("to", to.getToken().getInfo());
        object.put("amountOut", amountOut);
        object.put("fee", fee);
        object.put("impact", impact);
        object.put("cost", cost);
        object.put("pools", getPools());
        object.put("tokenPath", getTokenPath());
        return object.toJSONString();
    }

    public void addStep(Edge edge) {
        nodes.add(edge.getTo());
        Step nextStep = new Step();
        nextStep.setEdge(edge);
        steps.add(nextStep);
        cost += edge.getPool().cost();
    }

    public Step getCurrentStep() {
        return steps.isEmpty() ? null : steps.get(steps.size() - 1);
    }

    public String getPools() {
        return String.join(" -> ",
                           steps.stream()
                                .map(step -> step.getEdge().getPool().getName())
                                .collect(Collectors.toList()));
    }

    public String getTokenPath() {
        List<String> symbols = new ArrayList<>(steps.size() + 1);
        symbols.add(((IToken) from.getToken()).getSymbol());
        steps.forEach(step -> symbols.add(((IToken) step.getEdge()
                                                        .getTo()
                                                        .getToken()).getSymbol()));
        return String.join(" -> ", symbols);
    }

    public boolean isBackward(Edge edge) {
        // It's not reasonable step to same pool of current step or an old node
        return nodes.contains(edge.getTo()) || isDuplicateWithCurrent(edge);
    }

    public boolean isDuplicateWithCurrent(Edge edge) {
        Step currentStep = getCurrentStep();
        return null != currentStep && currentStep.getEdge().getPool().equals(edge.getPool());
    }

    @Data
    public static class Step {
        private BigInteger amountIn;
        private BigInteger amountOut;
        private Edge edge;
    }
}
