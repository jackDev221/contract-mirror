package org.tron.defi.contract_mirror.core.graph;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.tron.defi.contract_mirror.TestApplication;
import org.tron.defi.contract_mirror.config.TokenConfigList;
import org.tron.defi.contract_mirror.core.ContractManager;
import org.tron.defi.contract_mirror.core.pool.SunswapV1Pool;
import org.tron.defi.contract_mirror.core.token.ITRC20;
import org.tron.defi.contract_mirror.core.token.TRC20;
import org.tron.defi.contract_mirror.core.token.TRX;

import java.util.ArrayList;
import java.util.Arrays;

@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = TestApplication.class)
public class GraphTest {
    private final String poolAddress = "TQn9Y2khEsLJW1ChVWFMSMeRDow5KcbLSE";
    @Autowired
    private ContractManager contractManager;
    @Autowired
    private TokenConfigList tokenConfigList;
    private Graph graph;
    private Node trxNode;
    private SunswapV1Pool dummyPool;
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

        SunswapV1Pool pool = new SunswapV1Pool((ITRC20) dummyNode.getToken());
        Node newNode = new Node(pool);
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
        contractManager.initTRX();
        String usdtAddress = tokenConfigList.getTokens().get("USDT");
        TRC20 usdt = (TRC20) contractManager.registerContract(new TRC20(usdtAddress));
        graph = new Graph();
        dummyPool
            = (SunswapV1Pool) contractManager.registerContract(new SunswapV1Pool(poolAddress));
        dummyPool.setTokens(new ArrayList<>(Arrays.asList(TRX.getInstance(), usdt)));
        // dummy -> trx
        trxNode = new Node(TRX.getInstance());
        dummyNode = new Node(usdt);
        Edge edge = new Edge(dummyNode, trxNode, dummyPool);
        dummyNode.addOutEdge(edge);
        trxNode.addInEdge(edge);
        graph.addNode(trxNode);
    }
}
