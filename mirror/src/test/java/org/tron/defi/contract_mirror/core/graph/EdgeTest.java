package org.tron.defi.contract_mirror.core.graph;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.tron.defi.contract_mirror.core.pool.SunswapV1Pool;
import org.tron.defi.contract_mirror.core.token.TRC20;
import org.tron.defi.contract_mirror.core.token.TRX;
import org.tron.defi.contract_mirror.core.token.Token;


@Slf4j
@ExtendWith(SpringExtension.class)
public class EdgeTest {
    @Test
    public void isEqualTest() {
        final String dummyAddress = "";
        SunswapV1Pool dummyPool = new SunswapV1Pool(dummyAddress);
        Token dummyToken = new TRC20(dummyAddress);
        Node dummyNode = new Node(dummyToken);
        Node trxNode = new Node(TRX.getInstance());
        Edge dummyEdge = new Edge(trxNode, dummyNode, dummyPool);
        Assertions.assertTrue(dummyEdge.isEqual(new Edge(trxNode, dummyNode, dummyPool)));
        Assertions.assertFalse(dummyEdge.isEqual(new Edge(dummyNode, trxNode, dummyPool)));
    }
}
