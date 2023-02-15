package org.tron.defi.contract_mirror.core;

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
import org.tron.defi.contract_mirror.core.token.TRX;
import org.tron.defi.contract_mirror.core.token.Token;
import org.tron.defi.contract_mirror.utils.chain.ContractTrigger;

import java.util.concurrent.ConcurrentHashMap;

@Component
public class ContractManager {
    private final ConcurrentHashMap<String, Contract> contracts = new ConcurrentHashMap<>(50000);
    @Autowired
    ContractConfigList contractConfigList;
    @Autowired
    ContractTrigger contractTrigger;
    @Autowired
    Graph graph;

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
                case CURVE_4POOL:
                    initCurve(contractConfig.getAddress(), contractConfig.getType());
                    break;
                case PSM_POOL:
                    initPsm(contractConfig.getAddress());
                    break;
            }
        }
    }

    public void initTRX() {
        registerContract(TRX.getInstance());
    }

    public void initSunswapV1(String address) {
        SunswapV1Factory sunswapV1Factory
            = (SunswapV1Factory) registerContract(new SunswapV1Factory(address));
        if (!sunswapV1Factory.init(graph)) {
            throw new RuntimeException("Failed to initialize v1 factory");
        }
    }

    public void initSunswapV2(String address) {
        SunswapV2Factory sunswapV2Factory
            = (SunswapV2Factory) registerContract(new SunswapV2Factory(address));
        if (!sunswapV2Factory.init(graph)) {
            throw new RuntimeException("Failed to initialize v2 factory");
        }
    }

    public void initCurve(String address, ContractType type) {
        Pool pool;
        switch (type) {
            case CURVE_2POOL:
            case CURVE_3POOL:
                pool = (Pool) registerContract(new CurvePool(address,
                                                             PoolType.convertFromContractType(type)));
                break;
            case CURVE_4POOL:
                pool = (Pool) registerContract(new Curve4Pool(address));
                break;
            default:
                throw new IllegalArgumentException(type.name());
        }
        if (!pool.init()) {
            throw new RuntimeException("Failed to init " +
                                       pool.getType() +
                                       " " +
                                       pool.getAddress());
        }
        int n = pool.getTokens().size();
        for (int i = 0; i < n - 1; i++) {
            Token token0 = pool.getTokens().get(i);
            Node node0 = graph.getNode(token0.getAddress());
            if (null == node0) {
                node0 = graph.addNode(new Node(token0));
            }
            for (int j = i + 1; j < n; j++) {
                Token token1 = pool.getTokens().get(j);
                Node node1 = graph.getNode(token1.getAddress());
                if (null == node1) {
                    node1 = graph.addNode(new Node(token1));
                }
                node0.addEdge(new Edge(node0, node1, pool));
                node1.addEdge(new Edge(node1, node0, pool));
            }
        }
    }

    public void initPsm(String address) {
        PsmPool pool = (PsmPool) registerContract(new PsmPool(address));
        if (!pool.init()) {
            throw new RuntimeException("Failed to init " +
                                       pool.getType() +
                                       " " +
                                       pool.getAddress());
        }
        Token usdd = pool.getUsdd();
        Node node0 = graph.getNode(usdd.getAddress());
        if (null == node0) {
            node0 = graph.addNode(new Node(usdd));
        }
        Token gem = pool.getGem();
        Node node1 = graph.getNode(gem.getAddress());
        if (null == node1) {
            node1 = graph.addNode(new Node(gem));
        }
        node0.addEdge(new Edge(node0, node1, pool));
        node1.addEdge(new Edge(node1, node0, pool));
    }

    public Contract getContract(String address) {
        return contracts.getOrDefault(address, null);
    }

    public Contract registerContract(Contract contract) {
        contract.setContractTrigger(contractTrigger);
        contract.setContractManager(this);
        Contract exist = contracts.putIfAbsent(contract.getAddress(), contract);
        return null != exist ? exist : contract;
    }
}
