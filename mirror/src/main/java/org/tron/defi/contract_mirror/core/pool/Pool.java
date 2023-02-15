package org.tron.defi.contract_mirror.core.pool;

import com.alibaba.fastjson.JSONObject;
import lombok.Getter;
import org.tron.defi.contract_mirror.core.Contract;
import org.tron.defi.contract_mirror.core.token.Token;

import java.util.ArrayList;

public abstract class Pool extends Contract implements Synchronizable {
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

    public abstract boolean init();

    @Override
    public String getContractType() {
        return getType().name();
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
    protected JSONObject getInfo() {
        JSONObject info = super.getInfo();
        info.put("name", getName());
        info.put("tokens", getTokens());
        info.put("lp_token", getLpToken());
        return info;
    }

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
