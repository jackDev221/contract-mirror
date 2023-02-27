package org.tron.defi.contract_mirror.core.pool;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import lombok.Getter;
import org.tron.defi.contract_mirror.core.SynchronizableContract;
import org.tron.defi.contract_mirror.core.token.Token;

import java.util.ArrayList;
import java.util.stream.Collectors;

public abstract class Pool extends SynchronizableContract {
    @Getter
    protected String name;
    @Getter
    protected PoolType type = PoolType.UNKNOWN;
    @Getter
    protected ArrayList<Token> tokens = new ArrayList<>();
    @Getter
    protected Token lpToken;

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
                                          .map(x -> x.getInfo())
                                          .collect(Collectors.toList())));
        if (null != getLpToken()) {
            info.put("lp_token", getLpToken().getInfo());
        }
        return info;
    }

    @Override
    public Boolean isReady() {
        return null;
    }

    @Override
    public Boolean isEventAccept() {
        return null;
    }

    @Override
    public void sync() {
        timestamp0 = System.currentTimeMillis();
        getContractData();
        timestamp1 = System.currentTimeMillis();
        timestamp2 = 2 * timestamp1 - timestamp0;
    }

    public abstract void init();

    protected abstract void getContractData();

    public void setTokens(ArrayList<Token> tokens) {
        if (tokens.size() <= 1) {
            throw new IllegalArgumentException("at least 2 tokens");
        }
        this.tokens = tokens;
        updateName();
    }

    protected void updateName() {
        name = type.name().concat(" ");
        for (int i = 0; i < tokens.size(); i++) {
            name = name.concat(tokens.get(i).getSymbol());
            if (i < tokens.size() - 1) {
                name = name.concat("-");
            }
        }
    }
}
