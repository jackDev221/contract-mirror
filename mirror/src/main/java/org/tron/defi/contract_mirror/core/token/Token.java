package org.tron.defi.contract_mirror.core.token;

import com.alibaba.fastjson.JSONObject;
import org.tron.defi.contract_mirror.core.Contract;

public abstract class Token extends Contract {
    protected String symbol;
    protected int decimals = 0;

    public Token(String address) {
        super(address);
    }

    @Override
    public String run(String method) {
        if (0 == method.compareTo("symbol")) {
            return getSymbol();
        } else if (0 == method.compareTo("decimals")) {
            return String.valueOf(getDecimals());
        } else {
            return super.run(method);
        }
    }

    @Override
    protected JSONObject getInfo() {
        JSONObject info = super.getInfo();
        info.put("symbol", getSymbol());
        info.put("decimals", getDecimals());
        return info;
    }

    public String getSymbol() {
        return symbol;
    }

    public int getDecimals() {
        return decimals;
    }
}
