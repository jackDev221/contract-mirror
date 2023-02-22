package org.tron.defi.contract_mirror.core.graph;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.tron.defi.contract_mirror.core.pool.SunswapV1Pool;
import org.tron.defi.contract_mirror.core.token.TRC20;
import org.tron.defi.contract_mirror.core.token.TRX;

import java.util.ArrayList;

@Slf4j
@ExtendWith(SpringExtension.class)
public class NodeTest {
    private final Node trxNode = new Node(TRX.getInstance());
    private final Node dummyNode = new Node(new TRC20(""));
    private final SunswapV1Pool dummyPool = new SunswapV1Pool("");

    @BeforeEach
    void setUp() {
        dummyNode.addOutEdge(new Edge(dummyNode, trxNode, dummyPool));
    }

    @Test
    public void addEdgeTest() {
        Node node = new Node(new TRC20(""));
        Edge edge = new Edge(node, trxNode, dummyPool);

        node.addOutEdge(edge);
        ArrayList<Edge> edges = node.getOutEdges();
        Assertions.assertEquals(1, edges.size());
        Assertions.assertEquals(edge, edges.get(0));

        node.addOutEdge(edge);
        edges = node.getOutEdges();
        Assertions.assertEquals(1, edges.size());
    }
}
