package org.tron.defi.contract_mirror.core;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.tron.defi.contract_mirror.common.ContractType;
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
                case SUNSWAP_FACTORY_V1:
                    initSunswapV1(contractConfig.getAddress());
                    break;
                case SUNSWAP_FACTORY_V2:
                    initSunswapV2(contractConfig.getAddress());
                    break;
                case CURVE_2POOL:
                case CURVE_3POOL:
                case CURVE_COMBINATION_4POOL:
                    initCurve(contractConfig.getAddress(), contractConfig.getType());
                    break;
                case PSM_POOL:
                    initPsm(contractConfig.getAddress(), contractConfig.getPolyAddress());
                    break;
            }
        }
    }

    public void initCurve(String address, ContractType type) {
        PoolType poolType = PoolType.convertFromContractType(type);
        Pool pool;
        switch (type) {
            case CURVE_2POOL:
            case CURVE_3POOL:
                pool = (Pool) registerContract(new CurvePool(address, poolType));
                break;
            case CURVE_COMBINATION_4POOL:
                pool = (Pool) registerContract(new CurveCombinationPool(address, poolType));
                break;
            default:
                throw new IllegalArgumentException(type.name());
        }
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
                node0.addOutEdge(new Edge(node0, node1, pool));
                node1.addOutEdge(new Edge(node1, node0, pool));
            }
        }
        log.info("INIT CURVE " + pool.info());
    }

    public void initPsm(String address, String polyAddress) {
        PsmPool pool = (PsmPool) registerContract(new PsmPool(address, polyAddress));
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
        node0.addOutEdge(new Edge(node0, node1, pool));
        node1.addOutEdge(new Edge(node1, node0, pool));
        log.info("INIT PSM " + pool.info());
    }

    public void initSunswapV1(String address) {
        SunswapV1Factory sunswapV1Factory
            = (SunswapV1Factory) registerContract(new SunswapV1Factory(address));
        sunswapV1Factory.setGraph(graph);
        sunswapV1Factory.sync();
        log.info("INIT SunswapV1 Factory " + sunswapV1Factory.info());
    }

    public void initSunswapV2(String address) {
        SunswapV2Factory sunswapV2Factory
            = (SunswapV2Factory) registerContract(new SunswapV2Factory(address));
        sunswapV2Factory.setGraph(graph);
        sunswapV2Factory.sync();
        log.info("INIT SunswapV2 Factory " + sunswapV2Factory.info());
    }

    public void initTRX() {
        registerContract(TRX.getInstance());
        log.info("INIT TRX " + TRX.getInstance().info());
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
                log.warn(pool.getAddress() +
                         " from " +
                         exist.getClass().getName() +
                         " to " +
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
