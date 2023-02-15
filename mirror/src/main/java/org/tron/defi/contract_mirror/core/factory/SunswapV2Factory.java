package org.tron.defi.contract_mirror.core.factory;

import com.alibaba.fastjson.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.tron.defi.contract_mirror.common.ContractType;
import org.tron.defi.contract_mirror.core.Contract;
import org.tron.defi.contract_mirror.core.graph.Edge;
import org.tron.defi.contract_mirror.core.graph.Graph;
import org.tron.defi.contract_mirror.core.graph.Node;
import org.tron.defi.contract_mirror.core.pool.Pool;
import org.tron.defi.contract_mirror.core.pool.SunswapV2Pool;
import org.tron.defi.contract_mirror.core.token.Token;
import org.tron.defi.contract_mirror.utils.abi.AbiDecoder;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

@Slf4j
public class SunswapV2Factory extends Contract {
    private final ReadWriteLock rwlock = new ReentrantReadWriteLock();
    private final Lock rlock = rwlock.readLock();
    private final Lock wlock = rwlock.writeLock();
    Graph graph;
    private ArrayList<Pool> pairs;
    private ConcurrentHashMap<String, ConcurrentHashMap<String, Pool>> pairMap;

    public SunswapV2Factory(String address) {
        super(address);
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
    protected JSONObject getInfo() {
        JSONObject info = super.getInfo();
        info.put("poolNum", getAllPairLen());
        return info;
    }

    @Override
    public String getContractType() {
        return ContractType.SUNSWAP_FACTORY_V2.name();
    }

    public boolean init(Graph graph) {
        this.graph = graph;
        int allPairLen = getAllPairLen();
        if (allPairLen == 0) {
            return false;
        }
        pairs = new ArrayList<>(allPairLen);
        pairMap = new ConcurrentHashMap<>(allPairLen);

        for (int i = 0; i < allPairLen; i++) {
            try {
                SunswapV2Pool pair = (SunswapV2Pool) getPair(i);
                if (null == pair) {
                    continue;
                }
                Token token0 = pair.getToken0();
                Token token1 = pair.getToken1();
                if (null == token0 || null == token1) {
                    return false;
                }
                Node node0 = graph.getNode(token0.getAddress());
                if (null == node0) {
                    node0 = graph.addNode(new Node(token0));
                }
                Node node1 = graph.getNode(token1.getAddress());
                if (null == node1) {
                    node1 = graph.addNode(new Node(token1));
                }
                node0.addEdge(new Edge(node0, node1, pair));
                node1.addEdge(new Edge(node1, node0, pair));
            } catch (RuntimeException exception) {
                return false;
            }
        }
        return true;
    }

    public int getAllPairLen() throws RuntimeException {
        if (pairs.size() > 0) {
            return pairs.size();
        }
        return getAllPairLenFromChain();
    }

    private int getAllPairLenFromChain() throws RuntimeException {
        final String SIGNATURE = "allPairsLength()";
        String result = contractTrigger.triggerConstant(getAddress(), SIGNATURE);
        if (result.isEmpty()) {
            throw new RuntimeException();
        }
        return Integer.parseInt(result, 16);
    }

    public Pool getPair(int id) throws RuntimeException {
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
            pair = (SunswapV2Pool) getPairFromChain(id);
        } catch (ClassCastException e) {
            handleInvalidPair(id);
        }
        wlock.lock();
        try {
            if (id == pairs.size()) {
                pairs.add(pair);
                Token token0 = pair.getToken0();
                Token token1 = pair.getToken1();
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
            }
        } finally {
            wlock.unlock();
        }
        return pair;
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

    private Pool getPairFromChain(int id) {
        final String SIGNATURE = "allPairs(uint256)";
        String result = contractTrigger.triggerConstant(getAddress(),
                                                        SIGNATURE,
                                                        String.valueOf(id));
        if (result.isEmpty()) {
            throw new RuntimeException();
        }
        String pairAddress = AbiDecoder.DecodeAddress(result).left;
        SunswapV2Pool pair;
        try {
            Contract contract = contractManager.getContract(pairAddress);
            pair = contract != null
                   ? (SunswapV2Pool) contract
                   : (SunswapV2Pool) contractManager.registerContract(new SunswapV2Pool(pairAddress));
        } catch (ClassCastException e) {
            log.error(e.getMessage());
            throw e;
        }
        Token token0 = pair.getToken0();
        Token token1 = pair.getToken1();
        pair.setTokens(new ArrayList<>(Arrays.asList(token0, token1)));
        pair.init();
        return pair;
    }

    public Pool getPair(String token0, String token1) {
        ConcurrentHashMap<String, Pool> concurrentHashMap = pairMap.getOrDefault(token0, null);
        if (null == concurrentHashMap) {
            return null;
        }
        return concurrentHashMap.getOrDefault(token1, null);
    }
}
