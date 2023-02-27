package org.tron.defi.contract_mirror.core.factory;

import com.alibaba.fastjson.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.tron.defi.contract.abi.ContractAbi;
import org.tron.defi.contract.abi.factory.SunswapV2FactoryAbi;
import org.tron.defi.contract_mirror.common.ContractType;
import org.tron.defi.contract_mirror.core.Contract;
import org.tron.defi.contract_mirror.core.SynchronizableContract;
import org.tron.defi.contract_mirror.core.graph.Edge;
import org.tron.defi.contract_mirror.core.graph.Graph;
import org.tron.defi.contract_mirror.core.graph.Node;
import org.tron.defi.contract_mirror.core.pool.Pool;
import org.tron.defi.contract_mirror.core.pool.SunswapV2Pool;
import org.tron.defi.contract_mirror.core.token.Token;
import org.tron.defi.contract_mirror.utils.chain.AddressConverter;
import org.web3j.abi.EventValues;
import org.web3j.abi.datatypes.Address;
import org.web3j.abi.datatypes.Type;
import org.web3j.abi.datatypes.generated.Uint256;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

@Slf4j
public class SunswapV2Factory extends SynchronizableContract {
    private final ReadWriteLock rwlock = new ReentrantReadWriteLock();
    private final Lock rlock = rwlock.readLock();
    private final Lock wlock = rwlock.writeLock();
    private final ArrayList<Pool> pairs = new ArrayList<>(10000);
    private final ConcurrentHashMap<String, ConcurrentHashMap<String, Pool>> pairMap
        = new ConcurrentHashMap<>(10000);
    Graph graph;

    public SunswapV2Factory(String address) {
        super(address);
    }

    @Override
    public ContractAbi loadAbi() {
        return tronContractTrigger.contractAt(SunswapV2FactoryAbi.class, getAddress());
    }

    @Override
    public String run(String method) {
        if (0 == method.compareTo("getAllPairLen")) {
            return String.valueOf(getAllPairLen());
        } else {
            return super.run(method);
        }
    }

    @Override
    public JSONObject getInfo() {
        JSONObject info = super.getInfo();
        info.put("poolNum", getAllPairLen());
        return info;
    }

    @Override
    public String getContractType() {
        return ContractType.SUNSWAP_FACTORY_V2.name();
    }

    @Override
    public void sync() {
        timestamp0 = System.currentTimeMillis();
        int allPairLength = getAllPairLenFromChain();
        long t1 = System.currentTimeMillis();
        wlock.lock();
        int currentLength = pairs.size();
        try {
            for (int i = allPairLength; i < currentLength; i++) {
                Pool pair = pairs.get(i);
                if (null == pair) {
                    continue;
                }
                Token token0 = pair.getTokens().get(0);
                Token token1 = pair.getTokens().get(1);
                Node node0 = graph.getNode(token0.getAddress());
                Node node1 = graph.getNode(token1.getAddress());
                Edge edge0 = new Edge(node0, node1, pair);
                Edge edge1 = new Edge(node1, node0, pair);
                graph.deleteEdge(edge0);
                graph.deleteEdge(edge1);
                if (null == graph.getNode(token0.getAddress())) {
                    contractManager.unregisterContract(token0);
                }
                if (null == graph.getNode(token1.getAddress())) {
                    contractManager.unregisterContract(token1);
                }
                contractManager.unregisterContract(pair);
            }
        } finally {
            wlock.unlock();
        }
        if (allPairLength > currentLength) {
            init(graph, currentLength, allPairLength);
        }
        timestamp1 = t1;
    }

    @Override
    protected void handleEvent(String eventName, EventValues eventValues, long eventTime) {
        if (eventName.equals("PairCreated")) {
            handleCreatePairEvent(eventValues);
        } else {
            // do nothing
        }
    }

    public int getAllPairLen() {
        if (pairs.size() > 0) {
            return pairs.size();
        }
        return getAllPairLenFromChain();
    }

    public Pool getPairById(String token0, String token1) {
        ConcurrentHashMap<String, Pool> concurrentHashMap = pairMap.getOrDefault(token0, null);
        if (null == concurrentHashMap) {
            return null;
        }
        return concurrentHashMap.getOrDefault(token1, null);
    }

    public Pool getPairById(int id) {
        SunswapV2Pool pair = null;
        rlock.lock();
        try {
            if (pairs.size() > id) {
                pair = (SunswapV2Pool) pairs.get(id);
            }
        } finally {
            rlock.unlock();
        }
        if (null != pair) {
            return pair;
        }
        try {
            return getPairFromChain(id);
        } catch (ClassCastException e) {
            handleInvalidPair(id);
            return null;
        }
    }

    public void init(Graph graph, int minId, int maxId) {
        this.graph = graph;
        maxId = Math.min(getAllPairLen(), maxId);
        if (maxId == 0) {
            return;
        }

        for (int i = minId; i < maxId; i++) {
            SunswapV2Pool pair = (SunswapV2Pool) getPairById(i);
            if (null == pair) {
                continue; // invalid pair
            }
            newPair(pair.getAddress(), i);
        }
    }

    private int getAllPairLenFromChain() {
        List<Type> response = abi.invoke(SunswapV2FactoryAbi.Functions.ALL_PAIRS_LENGTH,
                                         Collections.emptyList());
        return ((Uint256) response.get(0)).getValue().intValue();
    }

    private Pool getPairByAddress(String pairAddress) {
        try {
            Contract contract = contractManager.getContract(pairAddress);
            SunswapV2Pool pair = contract != null
                                 ? (SunswapV2Pool) contract
                                 :
                                 (SunswapV2Pool) contractManager.registerContract(new SunswapV2Pool(
                                     pairAddress));
            Token token0 = pair.getToken0();
            Token token1 = pair.getToken1();
            pair.setTokens(new ArrayList<>(Arrays.asList(token0, token1)));
            pair.init();
            return pair;
        } catch (ClassCastException e) {
            log.error(e.getMessage());
            throw e;
        }
    }

    private Pool getPairFromChain(int id) {
        List<Type> response = abi.invoke(SunswapV2FactoryAbi.Functions.ALL_PAIRS,
                                         Collections.singletonList(id));
        String pairAddress
            = AddressConverter.EthToTronBase58Address(((Address) response.get(0)).getValue());
        return getPairByAddress(pairAddress);
    }

    private void handleCreatePairEvent(EventValues eventValues) {
        String poolAddress
            = AddressConverter.EthToTronBase58Address(((Address) eventValues.getNonIndexedValues()
                                                                            .get(0)).getValue());
        int allPairLengh = ((Uint256) eventValues.getNonIndexedValues().get(1)).getValue()
                                                                               .intValue();
        if (allPairLengh != getAllPairLen() + 1) {
            // miss some event, need sync
            throw new IllegalStateException();
        }
        newPair(poolAddress, allPairLengh);
    }

    private void handleInvalidPair(int id) {
        wlock.lock();
        try {
            if (id == pairs.size()) {
                pairs.add(null);
            }
        } finally {
            wlock.unlock();
        }
    }

    private void newPair(String poolAddress, int newLength) {
        Pool pair = getPairByAddress(poolAddress);
        Token token0 = pair.getTokens().get(0);
        Token token1 = pair.getTokens().get(1);
        if (token0 == null || token1 == null) {
            // this shouldn't happen
            throw new IllegalStateException();
        }

        wlock.lock();
        try {
            if (newLength != pairs.size() + 1) {
                throw new IllegalStateException();
            }
            pairs.add(pair);
            ConcurrentHashMap<String, Pool> concurrentHashMap
                = pairMap.getOrDefault(token0.getAddress(), null);
            if (null == concurrentHashMap) {
                concurrentHashMap = new ConcurrentHashMap<>();
                if (null != pairMap.putIfAbsent(token0.getAddress(), concurrentHashMap)) {
                    concurrentHashMap = pairMap.get(token0.getAddress());
                }
            }
            concurrentHashMap.put(token1.getAddress(), pair);
            concurrentHashMap = pairMap.getOrDefault(token1.getAddress(), null);
            if (null == concurrentHashMap) {
                concurrentHashMap = new ConcurrentHashMap<>();
                if (null != pairMap.putIfAbsent(token1.getAddress(), concurrentHashMap)) {
                    concurrentHashMap = pairMap.get(token1.getAddress());
                }
            }
            concurrentHashMap.put(token0.getAddress(), pair);
        } finally {
            wlock.unlock();
        }

        Node node0 = graph.getNode(token0.getAddress());
        if (null == node0) {
            node0 = graph.addNode(new Node(token0));
        }
        Node node1 = graph.getNode(token1.getAddress());
        if (null == node1) {
            node1 = graph.addNode(new Node(token1));
        }
        Edge edge0 = new Edge(node0, node1, pair);
        Edge edge1 = new Edge(node1, node0, pair);
        node0.addInEdge(edge1);
        node0.addOutEdge(edge0);
        node1.addInEdge(edge0);
        node1.addOutEdge(edge1);
    }
}
