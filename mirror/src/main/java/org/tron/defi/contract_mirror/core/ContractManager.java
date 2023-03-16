package org.tron.defi.contract_mirror.core;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.tron.defi.contract_mirror.config.ContractConfigList;
import org.tron.defi.contract_mirror.core.factory.SunswapV1Factory;
import org.tron.defi.contract_mirror.core.factory.SunswapV2Factory;
import org.tron.defi.contract_mirror.core.graph.Edge;
import org.tron.defi.contract_mirror.core.graph.Graph;
import org.tron.defi.contract_mirror.core.graph.Node;
import org.tron.defi.contract_mirror.core.pool.*;
import org.tron.defi.contract_mirror.core.token.ITRC20;
import org.tron.defi.contract_mirror.core.token.TRX;
import org.tron.defi.contract_mirror.utils.chain.TronContractTrigger;

import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
public class ContractManager {
    private final ConcurrentHashMap<String, Contract> contracts = new ConcurrentHashMap<>(50000);
    @Autowired
    ContractConfigList contractConfigList;
    @Autowired
    TronContractTrigger tronContractTrigger;
    @Autowired
    Graph graph;

    public Contract getContract(String address) {
        return contracts.getOrDefault(address, null);
    }

    public void init() {
        initTRX();
        for (ContractConfigList.ContractConfig contractConfig : contractConfigList.getContracts()) {
            switch (contractConfig.getType()) {
                case WTRX_TOKEN:
                    initWTRX(contractConfig.getAddress());
                    break;
                case SUNSWAP_FACTORY_V1:
                    initSunswapV1(contractConfig.getAddress());
                    break;
                case SUNSWAP_FACTORY_V2:
                    initSunswapV2(contractConfig.getAddress());
                    break;
                case CURVE_2POOL:
                case CURVE_3POOL:
                case CURVE_COMBINATION_4POOL:
                    initCurve(contractConfig);
                    break;
                case PSM_POOL:
                    initPsm(contractConfig);
                    break;
            }
        }
    }

    public void initTRX() {
        registerContract(TRX.getInstance());
        graph.addNode(new Node(TRX.getInstance()));
        log.info("INIT TRX {}", TRX.getInstance().info());
    }

    public void initCurve(ContractConfigList.ContractConfig config) {
        PoolType poolType = PoolType.convertFromContractType(config.getType());
        Pool pool;
        switch (config.getType()) {
            case CURVE_2POOL:
            case CURVE_3POOL:
                pool = (Pool) registerContract(new CurvePool(config.getAddress(), poolType));
                break;
            case CURVE_COMBINATION_4POOL:
                pool = (Pool) registerContract(new CurveCombinationPool(config.getAddress(),
                                                                        poolType));
                break;
            default:
                throw new IllegalArgumentException(config.getType().name());
        }
        pool.setName(config.getName());
        pool.init();
        int n = pool.getTokens().size();
        for (int i = 0; i < n - 1; i++) {
            Contract token0 = pool.getTokens().get(i);
            Node node0 = graph.getNode(token0.getAddress());
            if (null == node0) {
                node0 = graph.addNode(new Node(token0));
            }
            for (int j = i + 1; j < n; j++) {
                Contract token1 = pool.getTokens().get(j);
                Node node1 = graph.getNode(token1.getAddress());
                if (null == node1) {
                    node1 = graph.addNode(new Node(token1));
                }
                Edge edge0 = new Edge(node0, node1, pool);
                Edge edge1 = new Edge(node1, node0, pool);
                node0.addInEdge(edge1);
                node0.addOutEdge(edge0);
                node1.addInEdge(edge0);
                node1.addOutEdge(edge1);
            }
        }
        if (PoolType.CURVE_COMBINATION4 == pool.getType()) {
            Node node0 = graph.getNode(pool.getTokens().get(0).getAddress());
            ITRC20 underlyingLpToken = ((CurveCombinationPool) pool).getUnderlyingPool()
                                                                    .getLpToken();
            Node node1 = graph.getNode(((Contract) underlyingLpToken).getAddress());
            if (null == node1) {
                node1 = graph.addNode(new Node((Contract) underlyingLpToken));
            }
            Edge edge0 = new Edge(node0, node1, pool);
            Edge edge1 = new Edge(node1, node0, pool);
            node0.addInEdge(edge1);
            node0.addOutEdge(edge0);
            node1.addInEdge(edge0);
            node1.addOutEdge(edge1);
        }
        log.info("INIT CURVE {}", pool.info());
    }

    public void initPsm(ContractConfigList.ContractConfig config) {
        PsmPool pool = (PsmPool) registerContract(new PsmPool(config.getAddress(),
                                                              config.getPolyAddress()));
        pool.setName(config.getName());
        pool.init();
        Contract usdd = (Contract) pool.getUsdd();
        Node node0 = graph.getNode(usdd.getAddress());
        if (null == node0) {
            node0 = graph.addNode(new Node(usdd));
        }
        Contract gem = (Contract) pool.getGem();
        Node node1 = graph.getNode(gem.getAddress());
        if (null == node1) {
            node1 = graph.addNode(new Node(gem));
        }
        Edge edge0 = new Edge(node0, node1, pool);
        Edge edge1 = new Edge(node1, node0, pool);
        node0.addInEdge(edge1);
        node0.addOutEdge(edge0);
        node1.addInEdge(edge0);
        node1.addOutEdge(edge1);
        log.info("INIT PSM {}", pool.info());
    }

    public void initSunswapV1(String address) {
        SunswapV1Factory sunswapV1Factory
            = (SunswapV1Factory) registerContract(new SunswapV1Factory(address));
        sunswapV1Factory.setGraph(graph);
        sunswapV1Factory.sync();
        log.info("INIT SunswapV1 Factory {}", sunswapV1Factory.info());
    }

    public void initSunswapV2(String address) {
        SunswapV2Factory sunswapV2Factory
            = (SunswapV2Factory) registerContract(new SunswapV2Factory(address));
        sunswapV2Factory.setGraph(graph);
        sunswapV2Factory.sync();
        log.info("INIT SunswapV2 Factory {}", sunswapV2Factory.info());
    }

    public void initWTRX(String address) {
        WTRX wtrx = (WTRX) registerContract(new WTRX(address));
        wtrx.init();
        Node trxNode = graph.getNode(TRX.getInstance().getAddress());
        assert null != trxNode;
        Node wtrxNode = graph.getNode(wtrx.getAddress());
        if (null == wtrxNode) {
            wtrxNode = graph.addNode(new Node(wtrx));
        }
        Edge edge0 = new Edge(trxNode, wtrxNode, wtrx);
        Edge edge1 = new Edge(wtrxNode, trxNode, wtrx);
        trxNode.addInEdge(edge1);
        trxNode.addOutEdge(edge0);
        wtrxNode.addInEdge(edge0);
        wtrxNode.addOutEdge(edge1);
        log.info("INIT WTRX {}", wtrx.getInfo());
    }

    public Contract registerContract(Contract contract) {
        contract.setTronContractTrigger(tronContractTrigger);
        contract.setContractManager(this);
        Contract exist = contracts.putIfAbsent(contract.getAddress(), contract);
        return null != exist ? exist : contract;
    }

    public <T extends Pool> T registerOrReplacePool(T pool, Class<T> clz) {
        Contract exist = contracts.getOrDefault(pool.getAddress(), null);
        if (null != exist) {
            if (clz.isAssignableFrom(exist.getClass())) {
                return (T) exist;
            } else if (ITRC20.class.isAssignableFrom(exist.getClass())) {
                // wrap token
                log.warn("{} from {} to {}",
                         pool.getAddress(),
                         exist.getClass().getName(),
                         clz.getName());
                unregisterContract(exist);
                try {
                    pool = (T) registerContract(clz.getConstructor(ITRC20.class)
                                                   .newInstance((ITRC20) exist));
                } catch (Exception e) {
                    e.printStackTrace();
                    throw new RuntimeException();
                }
                return (T) graph.replaceNode(new Node(pool)).getToken();
            } else {
                throw new ClassCastException();
            }
        } else {
            return (T) registerContract(pool);
        }
    }

    public void unregisterContract(Contract contract) {
        contracts.remove(contract.getAddress());
    }
}
