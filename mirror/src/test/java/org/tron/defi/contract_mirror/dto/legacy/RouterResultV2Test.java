package org.tron.defi.contract_mirror.dto.legacy;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.tron.defi.contract_mirror.TestApplication;
import org.tron.defi.contract_mirror.config.ContractConfigList;
import org.tron.defi.contract_mirror.core.ContractManager;
import org.tron.defi.contract_mirror.core.ContractType;
import org.tron.defi.contract_mirror.core.graph.Edge;
import org.tron.defi.contract_mirror.core.graph.Graph;
import org.tron.defi.contract_mirror.core.graph.Node;
import org.tron.defi.contract_mirror.core.pool.Pool;
import org.tron.defi.contract_mirror.dao.RouterPath;

import java.math.BigInteger;
import java.util.Collections;

@Slf4j
@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = TestApplication.class)
public class RouterResultV2Test {
    @Autowired
    private ContractConfigList contractConfigList;
    @Autowired
    private ContractManager contractManager;
    @Autowired
    private Graph graph;
    private RouterPath path;

    @Test
    public void fromRouterPathTest() {
        RouterResultV2 resultV2 = RouterResultV2.fromRouterPath(path);
        log.info(resultV2.toString());
    }

    @BeforeEach
    public void setUp() {
        Pool pool = null;
        for (ContractConfigList.ContractConfig config : contractConfigList.getContracts()) {
            if (config.getType() == ContractType.CURVE_2POOL) {
                contractManager.initCurve(config);
                pool = (Pool) contractManager.getContract(config.getAddress());
                break;
            }
        }
        Node nodeFrom = graph.getNode("TR7NHqjeKQxGTCi8q8ZY4pL8otSzgjLj6t");
        Node nodeTo = graph.getNode("TPYmHEhy5n8TCEfYGqW2rPxsghSfzghPDn");
        path = new RouterPath(nodeFrom, BigInteger.valueOf(1000000), nodeTo);
        path.setAmountOut(BigInteger.valueOf(1024069553242042612L));
        path.setFee(BigInteger.valueOf(11946));
        path.setImpact(BigInteger.valueOf(-1508454719732594L));
        RouterPath.Step step = new RouterPath.Step();
        step.setEdge(new Edge(nodeFrom, nodeTo, pool));
        step.setAmountIn(path.getAmountIn());
        step.setAmountOut(path.getAmountOut());
        path.setSteps(Collections.singletonList(step));
    }
}
