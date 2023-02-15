package org.tron.defi.contract_mirror.core.graph;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.tron.defi.contract_mirror.TestApplication;
import org.tron.defi.contract_mirror.core.token.TRX;

@Slf4j
@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = TestApplication.class)
public class GraphTest {
    @Autowired
    private Graph graph;
    private Node trxNode;

    @BeforeEach
    public void setUp() {
        trxNode = new Node(TRX.getInstance());
        graph.addNode(trxNode);
    }

    @Test
    public void getNodeTest() {
        Assertions.assertNull(graph.getNode(""));
        Assertions.assertEquals(trxNode, graph.getNode(TRX.getInstance().getAddress()));
    }
}
