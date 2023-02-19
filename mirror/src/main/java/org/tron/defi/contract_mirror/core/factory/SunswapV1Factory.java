package org.tron.defi.contract_mirror.core.factory;

import com.alibaba.fastjson.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.tron.defi.contract.abi.ContractAbi;
import org.tron.defi.contract.abi.factory.SunswapV1FactoryAbi;
import org.tron.defi.contract_mirror.common.ContractType;
import org.tron.defi.contract_mirror.core.Contract;
import org.tron.defi.contract_mirror.core.SynchronizableContract;
import org.tron.defi.contract_mirror.core.graph.Edge;
import org.tron.defi.contract_mirror.core.graph.Graph;
import org.tron.defi.contract_mirror.core.graph.Node;
import org.tron.defi.contract_mirror.core.pool.Pool;
import org.tron.defi.contract_mirror.core.pool.SunswapV1Pool;
import org.tron.defi.contract_mirror.core.token.TRC20;
import org.tron.defi.contract_mirror.core.token.TRX;
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
public class SunswapV1Factory extends SynchronizableContract {
    private final ReadWriteLock rwlock = new ReentrantReadWriteLock();
    private final Lock rlock = rwlock.readLock();
    private final Lock wlock = rwlock.writeLock();
    Graph graph;
    private ConcurrentHashMap<String, Pool> pools;
    private ConcurrentHashMap<String, Token> tokenMap;
    private ArrayList<Token> tokens;

    public SunswapV1Factory(String address) {
        super(address);
    }

    @Override
    public ContractAbi loadAbi() {
        return tronContractTrigger.contractAt(SunswapV1FactoryAbi.class, getAddress());
    }

    @Override
    public String run(String method) {
        if (0 == method.compareTo("tokenCount")) {
            return String.valueOf(getTokenCount());
        } else {
            return super.run(method);
        }
    }

    @Override
    protected JSONObject getInfo() {
        JSONObject info = super.getInfo();
        info.put("poolNum", getTokenCount());
        return info;
    }

    @Override
    public String getContractType() {
        return ContractType.SUNSWAP_FACTORY_V1.name();
    }

    public boolean init(Graph graph) {
        this.graph = graph;
        int tokenCount = getTokenCount();
        if (tokenCount == 0) {
            return false;
        }
        Node trxNode = graph.getNode(TRX.getInstance().getAddress());
        if (null == trxNode) {
            trxNode = graph.addNode(new Node(TRX.getInstance()));
        }
        // TODO: parallelism optimization
        pools = new ConcurrentHashMap<>(tokenCount);
        tokens = new ArrayList<>(tokenCount);
        tokenMap = new ConcurrentHashMap<>(tokenCount);
        for (int i = 0; i < tokenCount; i++) {
            try {
                Token token = getTokenWithId(i);
                if (null == token) {
                    continue; // invalid token
                }
                if (token.getAddress().equals(TRX.getInstance().getAddress())) {
                    continue;
                }
                Pool pool = getExchange(token.getAddress());
                if (null == pool) {
                    return false;
                }
                Node node = graph.getNode(token.getAddress());
                if (null == node) {
                    node = graph.addNode(new Node(token));
                }
                trxNode.addEdge(new Edge(trxNode, node, pool));
                node.addEdge(new Edge(node, trxNode, pool));
            } catch (RuntimeException exception) {
                return false;
            }
        }
        return true;
    }

    public int getTokenCount() {
        return null == tokens ? getTokenCountFromChain() : tokens.size();
    }

    private int getTokenCountFromChain() {
        List<Type> response = abi.invoke(SunswapV1FactoryAbi.Functions.TOKEN_COUNT,
                                         Collections.emptyList());
        return ((Uint256) response.get(0)).getValue().intValue();
    }

    public Token getTokenWithId(int id) {
        Token token = null;
        rlock.lock();
        try {
            if (tokens.size() > id) {
                token = tokens.get(id);
            }
        } finally {
            rlock.unlock();
        }
        if (null != token) {
            return token;
        }
        token = getTokenWithIdFromChain(id);
        wlock.lock();
        try {
            if (id == tokens.size()) {
                tokens.add(token);
                tokenMap.put(token.getAddress(), token);
            }
        } finally {
            wlock.unlock();
        }
        return token;
    }

    private Token getTokenWithIdFromChain(int id) {
        List<Type> response = abi.invoke(SunswapV1FactoryAbi.Functions.GET_TOKEN_WITH_ID,
                                         Collections.singletonList(id));
        String tokenAddress
            = AddressConverter.EthToTronBase58Address(((Address) response.get(0)).getValue());
        try {
            Contract contract = contractManager.getContract(tokenAddress);
            return null != contract
                   ? (Token) contract
                   : (Token) contractManager.registerContract(new TRC20(tokenAddress));
        } catch (ClassCastException e) {
            // invalid token address
            log.error(e.getMessage());
            throw e;
        }
    }

    public Pool getExchange(String tokenAddress) {
        Pool pool = pools.getOrDefault(tokenAddress, null);
        if (null != pool) {
            return pool;
        }
        pool = getExchangeFromChain(tokenAddress);
        Pool exist = pools.putIfAbsent(tokenAddress, pool);
        return null != exist ? exist : pool;
    }

    private Pool getExchangeFromChain(String tokenAddress) {
        String ethAddress = AddressConverter.TronBase58ToEthAddress(tokenAddress);
        List<Type> response = abi.invoke(SunswapV1FactoryAbi.Functions.GET_EXCHANGE,
                                         Collections.singletonList(ethAddress));
        String poolAddress
            = AddressConverter.EthToTronBase58Address(((Address) response.get(0)).getValue());
        Pool pool
            = (SunswapV1Pool) contractManager.registerContract(new SunswapV1Pool(poolAddress));
        Contract contract = contractManager.getContract(tokenAddress);
        try {
            Token token = null != contract
                          ? (Token) contract
                          : (Token) contractManager.registerContract(new TRC20(tokenAddress));
            pool.setTokens(new ArrayList<>(Arrays.asList(TRX.getInstance(), token)));
            pool.init();
        } catch (ClassCastException e) {
            log.error(e.getMessage());
            throw e;
        }
        return pool;
    }

    @Override
    public void sync() {

    }

    @Override
    protected void handleEvent(String eventName, EventValues eventValues) {

    }
}
