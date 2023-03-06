package org.tron.defi.contract_mirror.core.factory;

import com.alibaba.fastjson.JSONObject;
import lombok.Setter;
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
import org.tron.defi.contract_mirror.core.token.ITRC20;
import org.tron.defi.contract_mirror.core.token.IToken;
import org.tron.defi.contract_mirror.core.token.TRC20;
import org.tron.defi.contract_mirror.core.token.TRX;
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
    private final ConcurrentHashMap<String, Pool> pools = new ConcurrentHashMap<>(20000);
    private final ConcurrentHashMap<String, ITRC20> tokenMap = new ConcurrentHashMap<>(20000);
    @Setter
    Graph graph;
    private List<Contract> tokens = new ArrayList<>(20000);

    public SunswapV1Factory(String address) {
        super(address);
    }

    @Override
    public String getContractType() {
        return ContractType.SUNSWAP_FACTORY_V1.name();
    }

    @Override
    public ContractAbi loadAbi() {
        return tronContractTrigger.contractAt(SunswapV1FactoryAbi.class, getAddress());
    }

    @Override
    public String run(String method) {
        switch (method) {
            case "tokenCount":
                return String.valueOf(getTokenCount());
            default:
                return super.run(method);
        }
    }

    @Override
    public JSONObject getInfo() {
        JSONObject info = super.getInfo();
        info.put("poolNum", getTokenCount());
        return info;
    }

    @Override
    public boolean isReady() {
        return isEventAccept();
    }

    @Override
    public void sync() {
        timestamp0 = System.currentTimeMillis();
        int tokenCount = getTokenCountFromChain();
        long t1 = System.currentTimeMillis();
        // remove tokens and pools
        wlock.lock();
        int currentCount = tokens.size();
        try {
            for (int i = tokenCount; i < currentCount; i++) {
                Contract token = tokens.get(i);
                Node trxNode = graph.getNode(TRX.getInstance().getAddress());
                Node node = graph.getNode(token.getAddress());
                Pool pool = getExchange(token.getAddress());
                graph.deleteEdge(new Edge(trxNode, node, pool));
                graph.deleteEdge(new Edge(node, trxNode, pool));
                if (null == graph.getNode(token.getAddress())) {
                    contractManager.unregisterContract(token);
                }
                contractManager.unregisterContract(pool);
                tokenMap.remove(token.getAddress());
            }
            if (tokenCount < currentCount) {
                tokens = tokens.subList(0, tokenCount);
            }
        } finally {
            wlock.unlock();
        }
        if (tokenCount > currentCount) {
            init(graph, currentCount, tokenCount);
        }
        timestamp1 = t1;
    }

    @Override
    protected void handleEvent(String eventName, EventValues eventValues, long eventTime) {
        switch (eventName) {
            case "NewExchange":
                handleNewExchangeEvent(eventValues);
                break;
            default:
                log.warn("Ignore event " + eventName);
                break;
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

    public int getTokenCount() {
        return tokens.isEmpty() ? getTokenCountFromChain() : tokens.size();
    }

    public IToken getTokenWithId(int id) {
        IToken token = null;
        rlock.lock();
        try {
            if (tokens.size() > id) {
                token = (IToken) tokens.get(id);
            }
        } finally {
            rlock.unlock();
        }
        if (null != token) {
            return token;
        }
        return getTokenWithIdFromChain(id);
    }

    public void init(Graph graph, int minId, int maxId) {
        this.graph = graph;
        maxId = Math.min(getTokenCount(), maxId);
        if (maxId == 0) {
            return;
        }
        if (null == graph.getNode(TRX.getInstance().getAddress())) {
            graph.addNode(new Node(TRX.getInstance()));
        }
        // TODO: parallelism optimization
        for (int i = minId; i < maxId; i++) {
            IToken token = getTokenWithId(i);
            if (null == token) {
                handleInvalidToken(i);
                continue; // invalid token
            }
            Contract contract = (Contract) token;
            if (contract.getAddress().equals(TRX.getInstance().getAddress())) {
                wlock.lock();
                tokens.add(contract);
                wlock.unlock();
                continue;
            }
            try {
                Pool pool = getExchange(contract.getAddress());
                newExchange(contract.getAddress(), pool.getAddress());
            } catch (RuntimeException e) {
                // TODO: is there any way to distinguish network error and invalid data ?
                handleInvalidToken(i);
            }
        }
    }

    private Pool getExchangeFromChain(String tokenAddress) {
        String ethAddress = AddressConverter.TronBase58ToEthAddress(tokenAddress);
        List<Type> response = abi.invoke(SunswapV1FactoryAbi.Functions.GET_EXCHANGE,
                                         Collections.singletonList(ethAddress));
        String poolAddress
            = AddressConverter.EthToTronBase58Address(((Address) response.get(0)).getValue());
        return getExchangeWithAddress(tokenAddress, poolAddress);
    }

    private Pool getExchangeWithAddress(String tokenAddress, String poolAddress) {
        Pool pool = contractManager.registerOrReplacePool(new SunswapV1Pool(poolAddress),
                                                          SunswapV1Pool.class);
        IToken token = getTokenWithAddress(tokenAddress);
        pool.setTokens(new ArrayList<>(Arrays.asList(TRX.getInstance(), (Contract) token)));
        pool.init();
        return pool;
    }

    private int getTokenCountFromChain() {
        List<Type> response = abi.invoke(SunswapV1FactoryAbi.Functions.TOKEN_COUNT,
                                         Collections.emptyList());
        return ((Uint256) response.get(0)).getValue().intValue();
    }

    private IToken getTokenWithAddress(String tokenAddress) {
        try {
            Contract contract = contractManager.getContract(tokenAddress);
            return null != contract
                   ? (IToken) contract
                   : (IToken) contractManager.registerContract(new TRC20(tokenAddress));
        } catch (ClassCastException e) {
            // invalid token address
            e.printStackTrace();
            log.error("INVALID TOKEN ADDRESS {}", tokenAddress);
            return null;
        }
    }

    private IToken getTokenWithIdFromChain(int id) {
        List<Type> response = abi.invoke(SunswapV1FactoryAbi.Functions.GET_TOKEN_WITH_ID,
                                         Collections.singletonList(id));
        String tokenAddress
            = AddressConverter.EthToTronBase58Address(((Address) response.get(0)).getValue());
        return getTokenWithAddress(tokenAddress);
    }

    private void handleInvalidToken(int id) {
        log.error("INVALID V1: {}", id);
        wlock.lock();
        if (id == tokens.size()) {
            tokens.add(null);
        }
        wlock.unlock();
    }

    private void handleNewExchangeEvent(EventValues eventValues) {
        log.info("handleNewExchangeEvent {}", getAddress());
        String tokenAddress
            = AddressConverter.EthToTronBase58Address(((Address) eventValues.getIndexedValues()
                                                                            .get(0)).getValue());
        String poolAddress
            = AddressConverter.EthToTronBase58Address(((Address) eventValues.getIndexedValues()
                                                                            .get(1)).getValue());
        newExchange(tokenAddress, poolAddress);
    }

    private void newExchange(String tokenAddress, String exchangeAddress) {
        Pool pool = getExchangeWithAddress(tokenAddress, exchangeAddress);
        Contract token = pool.getTokens().get(1);
        Node node = graph.getNode(tokenAddress);
        if (null == node) {
            node = graph.addNode(new Node(token));
        }
        Node trxNode = graph.getNode(TRX.getInstance().getAddress());
        Edge edge0 = new Edge(trxNode, node, pool);
        Edge edge1 = new Edge(node, trxNode, pool);
        trxNode.addInEdge(edge1);
        trxNode.addOutEdge(edge0);
        node.addInEdge(edge0);
        node.addOutEdge(edge1);

        wlock.lock();
        try {
            tokens.add(token);
            tokenMap.put(tokenAddress, (ITRC20) token);
        } finally {
            wlock.unlock();
        }
        log.info("New SunswapV1 {}", pool.info());
    }
}
