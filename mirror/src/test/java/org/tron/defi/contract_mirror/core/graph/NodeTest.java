package org.tron.defi.contract_mirror.core.graph;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.tron.defi.contract_mirror.core.pool.SunswapV1Pool;
import org.tron.defi.contract_mirror.core.token.TRC20;
import org.tron.defi.contract_mirror.core.token.TRX;

import java.util.ArrayList;

public class NodeTest {
    private Node trxNode;
    private Node dummyNode;
    private final SunswapV1Pool dummyPool = new SunswapV1Pool("");

    @Test
    public void addEdgeTest() {
        Node node = new Node(new TRC20("fake"));
        Edge edge = new Edge(node, trxNode, dummyPool);
        // normal case
        node.addOutEdge(edge);
        ArrayList<Edge> edges = node.getOutEdges();
        Assertions.assertEquals(1, edges.size());
        Assertions.assertEquals(edge, edges.get(0));
        Assertions.assertEquals(1, node.outDegree());
        // duplicate edge check
        node.addOutEdge(edge);
        edges = node.getOutEdges();
        Assertions.assertEquals(1, edges.size());
        Assertions.assertEquals(1, node.outDegree());
        // to node check
        node.addInEdge(edge);
        edges = node.getInEdges();
        Assertions.assertEquals(0, edges.size());
        Assertions.assertEquals(0, node.inDegree());
    }

    @Test
    public void deleteEdgeTest() {
        // dummy -> trx <-> node
        Node node = new Node(new TRC20("fake"));
        Edge inEdge = new Edge(trxNode, node, dummyPool);
        Edge outEdge = new Edge(node, trxNode, dummyPool);
        node.addInEdge(inEdge);
        node.addOutEdge(outEdge);
        trxNode.addInEdge(outEdge);
        trxNode.addOutEdge(inEdge);
        Assertions.assertEquals(1, node.inDegree());
        Assertions.assertEquals(1, node.outDegree());
        Assertions.assertEquals(2, trxNode.inDegree());
        Assertions.assertEquals(1, trxNode.outDegree());

        // dummmy -> trx <- node
        ArrayList<Edge> inEdges = node.getInEdges();
        Assertions.assertEquals(1, inEdges.size());
        node.deleteInEdge(inEdges.get(0));
        Assertions.assertEquals(1, inEdges.size());
        Assertions.assertEquals(0, node.getInEdges().size());
        Assertions.assertEquals(0, node.inDegree());

        ArrayList<Edge> outEdges = trxNode.getOutEdges();
        Assertions.assertEquals(1, outEdges.size());
        trxNode.deleteOutEdge(outEdges.get(0));
        Assertions.assertEquals(1, outEdges.size());
        Assertions.assertEquals(0, trxNode.getOutEdges().size());
        Assertions.assertEquals(0, trxNode.outDegree());
    }

    @Test
    public void isEqualTest() {
        Assertions.assertTrue(dummyNode.isEqual(new Node(new TRC20(""))));
    }

    @BeforeEach
    void setUp() {
        // dummy -> trx
        trxNode = new Node(TRX.getInstance());
        dummyNode = new Node(new TRC20(""));
        Edge edge = new Edge(dummyNode, trxNode, dummyPool);
        dummyNode.addOutEdge(edge);
        trxNode.addInEdge(edge);
    }
}
