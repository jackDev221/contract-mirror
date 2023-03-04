package org.tron.defi.contract_mirror.core.graph;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.tron.defi.contract_mirror.core.pool.SunswapV1Pool;
import org.tron.defi.contract_mirror.core.token.TRC20;
import org.tron.defi.contract_mirror.core.token.TRX;


public class EdgeTest {
    @Test
    public void isEqualTest() {
        final String dummyAddress = "";
        SunswapV1Pool dummyPool = new SunswapV1Pool(dummyAddress);
        TRC20 dummyToken = new TRC20(dummyAddress);
        Node dummyNode = new Node(dummyToken);
        Node trxNode = new Node(TRX.getInstance());
        Edge dummyEdge = new Edge(trxNode, dummyNode, dummyPool);
        Assertions.assertTrue(dummyEdge.isEqual(new Edge(trxNode, dummyNode, dummyPool)));
        Assertions.assertFalse(dummyEdge.isEqual(new Edge(dummyNode, trxNode, dummyPool)));
        Assertions.assertFalse(dummyEdge.isEqual(new Edge(trxNode,
                                                          new Node(dummyPool),
                                                          dummyPool)));
    }
}
