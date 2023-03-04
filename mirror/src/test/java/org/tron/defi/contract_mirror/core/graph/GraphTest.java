package org.tron.defi.contract_mirror.core.graph;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.tron.defi.contract_mirror.core.pool.SunswapV1Pool;
import org.tron.defi.contract_mirror.core.token.ITRC20;
import org.tron.defi.contract_mirror.core.token.TRC20;
import org.tron.defi.contract_mirror.core.token.TRX;

public class GraphTest {
    private Graph graph;
    private Node trxNode;
    private final SunswapV1Pool dummyPool = new SunswapV1Pool("");
    private Node dummyNode;

    @Test
    public void deleteEdgeTest() {
        Assertions.assertEquals(1, trxNode.inDegree());
        Assertions.assertEquals(1, dummyNode.outDegree());
        graph.deleteEdge(trxNode.getInEdges().get(0));
        Assertions.assertEquals(0, trxNode.inDegree());
        Assertions.assertEquals(0, trxNode.outDegree());
        Assertions.assertEquals(0, dummyNode.inDegree());
        Assertions.assertEquals(0, dummyNode.outDegree());
        Assertions.assertNull(graph.getNode(dummyNode.getToken().getAddress()));
        Assertions.assertNull(graph.getNode(trxNode.getToken().getAddress()));
    }

    @Test
    public void getNodeTest() {
        Assertions.assertNull(graph.getNode(dummyNode.getToken().getAddress()));
        Assertions.assertEquals(trxNode, graph.getNode(TRX.getInstance().getAddress()));
    }

    @Test
    public void replaceNode() {
        // dummy <-> trx
        Edge edge = new Edge(trxNode, dummyNode, dummyPool);
        trxNode.addOutEdge(edge);
        dummyNode.addInEdge(edge);
        graph.addNode(dummyNode);
        Assertions.assertEquals(1, trxNode.inDegree());
        Assertions.assertEquals(1, trxNode.outDegree());
        Assertions.assertEquals(1, dummyNode.inDegree());
        Assertions.assertEquals(1, dummyNode.outDegree());

        Node newNode = new Node(new SunswapV1Pool((ITRC20) dummyNode.getToken()));
        newNode = graph.replaceNode(newNode);
        Assertions.assertNotEquals(dummyNode, newNode);
        Assertions.assertEquals(0, dummyNode.inDegree());
        Assertions.assertEquals(0, dummyNode.outDegree());
        Assertions.assertEquals(newNode, graph.getNode(dummyNode.getToken().getAddress()));

        Assertions.assertEquals(1, trxNode.inDegree());
        Assertions.assertEquals(1, trxNode.outDegree());
        Assertions.assertEquals(newNode, trxNode.getInEdges().get(0).getFrom());
        Assertions.assertEquals(newNode, trxNode.getOutEdges().get(0).getTo());
        Assertions.assertEquals(1, newNode.inDegree());
        Assertions.assertEquals(1, newNode.outDegree());
        Assertions.assertEquals(trxNode, newNode.getInEdges().get(0).getFrom());
        Assertions.assertEquals(trxNode, newNode.getOutEdges().get(0).getTo());
    }

    @BeforeEach
    void setUp() {
        graph = new Graph();
        // dummy -> trx
        trxNode = new Node(TRX.getInstance());
        dummyNode = new Node(new TRC20(""));
        Edge edge = new Edge(dummyNode, trxNode, dummyPool);
        dummyNode.addOutEdge(edge);
        trxNode.addInEdge(edge);
        graph.addNode(trxNode);
    }
}
