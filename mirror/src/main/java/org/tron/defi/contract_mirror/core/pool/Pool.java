package org.tron.defi.contract_mirror.core.pool;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import lombok.Getter;
import lombok.Setter;
import org.tron.defi.contract_mirror.core.Contract;
import org.tron.defi.contract_mirror.core.SynchronizableContract;
import org.tron.defi.contract_mirror.core.token.ITRC20;
import org.tron.defi.contract_mirror.core.token.IToken;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Collectors;

public abstract class Pool extends SynchronizableContract {
    public static final int PRICE_DECIMALS = 36;
    public static final BigInteger PRICE_FACTOR = BigInteger.valueOf(10).pow(PRICE_DECIMALS);
    protected final ReadWriteLock rwlock = new ReentrantReadWriteLock();
    protected final Lock rlock = rwlock.readLock();
    protected final Lock wlock = rwlock.writeLock();
    @Getter
    @Setter
    protected String name;
    @Getter
    protected PoolType type = PoolType.UNKNOWN;
    @Getter
    protected ArrayList<Contract> tokens = new ArrayList<>();
    @Getter
    protected ITRC20 lpToken;
    protected boolean initialized = false;

    Pool(String address) {
        super(address);
    }

    @Override
    public String getContractType() {
        return getType().name();
    }

    @Override
    public JSONObject getInfo() {
        JSONObject info = super.getInfo();
        info.put("name", getName());
        info.put("tokens",
                 new JSONArray(getTokens().stream()
                                          .map(Contract::getInfo)
                                          .collect(Collectors.toList())));
        if (null != getLpToken()) {
            info.put("lp_token", ((Contract) getLpToken()).getInfo());
        }
        return info;
    }

    @Override
    public boolean isReady() {
        return isEventAccept();
    }

    @Override
    public void sync() {
        timestamp0 = System.currentTimeMillis();
        getContractData();
        timestamp1 = Math.max(timestamp0, System.currentTimeMillis()); // max to avoid clock drift
        timestamp2 = 2 * timestamp1 - timestamp0;
    }

    public abstract BigInteger getAmountOutUnsafe(IToken fromToken,
                                                  IToken toToken,
                                                  BigInteger amountIn);

    public abstract BigInteger getApproximateFee(IToken fromToken,
                                                 IToken toToken,
                                                 BigInteger amountIn);

    public abstract BigInteger getPrice(IToken fromToken, IToken toToken);

    protected abstract void doInitialize();

    protected abstract void getContractData();

    public int cost() {
        return 1;
    }

    public BigInteger getAmountOut(String from, String to, BigInteger amountIn) {
        IToken fromToken = getTokenByAddress(from);
        IToken toToken = getTokenByAddress(to);
        if (null == fromToken || null == toToken) {
            throw new IllegalArgumentException();
        }
        return getAmountOutUnsafe(fromToken, toToken, amountIn);
    }

    public IToken getTokenByAddress(String address) {
        for (Contract token : tokens) {
            if (token.getAddress().equals(address)) {
                return (IToken) token;
            }
        }
        return null;
    }

    public int getTokenId(String address) {
        for (int i = 0; i < tokens.size(); i++) {
            if (tokens.get(i).getAddress().equals(address)) {
                return i;
            }
        }
        throw new IllegalArgumentException();
    }

    public void init() {
        if (initialized) {
            return;
        }
        doInitialize();
        initialized = true;
    }

    public void replaceToken(Contract newToken) {
        for (int i = 0; i < tokens.size(); i++) {
            if (tokens.get(i).getAddress().equals(newToken.getAddress())) {
                tokens.set(i, newToken);
                break;
            }
        }
        updateName();
        sync();
    }

    public void setTokens(ArrayList<Contract> tokens) {
        if (tokens.size() <= 1) {
            throw new IllegalArgumentException("at least 2 tokens");
        }
        this.tokens = tokens;
        updateName();
    }

    protected void updateName() {
        name = type.name().concat(" ");
        for (int i = 0; i < tokens.size(); i++) {
            name = name.concat(((IToken) tokens.get(i)).getSymbol());
            if (i < tokens.size() - 1) {
                name = name.concat("-");
            }
        }
    }
}
