package org.tron.defi.contract_mirror.strategy;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.tron.defi.contract_mirror.core.graph.Edge;
import org.tron.defi.contract_mirror.core.graph.Node;
import org.tron.defi.contract_mirror.core.pool.SunswapV1Pool;
import org.tron.defi.contract_mirror.core.pool.SunswapV2Pool;
import org.tron.defi.contract_mirror.core.pool.WTRX;
import org.tron.defi.contract_mirror.core.token.TRC20;
import org.tron.defi.contract_mirror.core.token.TRX;
import org.tron.defi.contract_mirror.dao.RouterPath;
import org.tron.defi.contract_mirror.utils.MethodUtil;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.math.BigInteger;

public class DefaultStrategyTest {
    @Test
    public void checkWTRXPathTest() throws NoSuchMethodException, InvocationTargetException,
        IllegalAccessException {
        Method method = MethodUtil.getNonAccessibleMethod(DefaultStrategy.class,
                                                          "checkWTRXPath",
                                                          RouterPath.class,
                                                          Edge.class);
        TRC20 dummy = new TRC20("");
        WTRX wtrx = new WTRX("");
        TRX trx = TRX.getInstance();
        Node from = new Node(trx);
        Node wtrxNode = new Node(wtrx);
        Node to = new Node(dummy);
        RouterPath path = new RouterPath(from, BigInteger.ONE, to);
        Edge edge = new Edge(from, wtrxNode, wtrx);
        // wtrx pool -> ?
        Assertions.assertTrue((boolean) method.invoke(null, path, edge));
        path.addStep(edge);
        // wtrx pool
        Assertions.assertFalse((boolean) method.invoke(null, path, null));
        // wtrx pool -> v1 -> ?
        Assertions.assertFalse((boolean) method.invoke(null,
                                                       path,
                                                       new Edge(wtrxNode,
                                                                to,
                                                                new SunswapV1Pool(""))));
        // wtrx pool -> v2 -> ?
        edge = new Edge(wtrxNode, to, new SunswapV2Pool(""));
        Assertions.assertTrue((boolean) method.invoke(null, path, edge));
        path.addStep(edge);
        // wtrx pool -> v2
        Assertions.assertTrue((boolean) method.invoke(null, path, null));

        // v1 -> ?
        path = new RouterPath(to, BigInteger.ONE, from);
        edge = new Edge(to, wtrxNode, new SunswapV1Pool(("")));
        Assertions.assertTrue((boolean) method.invoke(null, path, edge));
        path.addStep(edge);
        // v1 -> wtrx pool -> ?
        edge = new Edge(wtrxNode, from, wtrx);
        Assertions.assertTrue((boolean) method.invoke(null, path, edge));
        path.addStep(edge);
        // v1 -> wtrx pool
        Assertions.assertFalse((boolean) method.invoke(null, path, null));
        // v1 -> wtrx pool -> v2 -> ?
        edge = new Edge(from, to, new SunswapV2Pool(""));
        Assertions.assertTrue((boolean) method.invoke(null, path, edge));
        path.addStep(edge);
        // v1 -> wtrx pool -> v2
        Assertions.assertTrue((boolean) method.invoke(null, path, null));

        // v2 -> ?
        path = new RouterPath(to, BigInteger.ONE, from);
        edge = new Edge(to, wtrxNode, new SunswapV2Pool(("")));
        Assertions.assertTrue((boolean) method.invoke(null, path, edge));
        path.addStep(edge);
        // v2 -> wtrx pool -> ?
        edge = new Edge(wtrxNode, from, wtrx);
        Assertions.assertTrue((boolean) method.invoke(null, path, edge));
        path.addStep(edge);
        // v2 -> wtrx pool
        Assertions.assertTrue((boolean) method.invoke(null, path, null));
        // v2 -> wtrx pool -> v1 -> ?
        edge = new Edge(from, to, new SunswapV1Pool(""));
        Assertions.assertTrue((boolean) method.invoke(null, path, edge));
        path.addStep(edge);
        // v2 -> wtrx pool -> v1
        Assertions.assertTrue((boolean) method.invoke(null, path, null));
    }
}
