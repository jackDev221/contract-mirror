package org.tron.defi.contract_mirror.core;

import com.alibaba.fastjson.JSONObject;
import lombok.Getter;
import lombok.Setter;
import org.tron.defi.contract_mirror.utils.chain.ContractTrigger;

public abstract class Contract {
    @Getter
    private final String address;
    @Setter
    protected ContractTrigger contractTrigger;
    @Setter
    protected ContractManager contractManager;

    public Contract(String address) {
        this.address = address;
    }

    public String run(String method) {
        throw new IllegalArgumentException("METHOD NOT EXISTS");
    }

    public String info() {
        return getInfo().toJSONString();
    }

    protected JSONObject getInfo() {
        JSONObject info = new JSONObject();
        info.put("address", getAddress());
        info.put("type", getContractType());
        return info;
    }

    public abstract String getContractType();
}
