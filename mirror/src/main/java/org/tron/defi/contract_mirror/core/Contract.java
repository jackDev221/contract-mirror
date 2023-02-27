package org.tron.defi.contract_mirror.core;

import com.alibaba.fastjson.JSONObject;
import lombok.Getter;
import lombok.Setter;
import org.tron.defi.contract.abi.ContractAbi;
import org.tron.defi.contract_mirror.utils.chain.TronContractTrigger;

public abstract class Contract {
    @Getter
    private final String address;
    protected TronContractTrigger tronContractTrigger;
    @Setter
    protected ContractManager contractManager;
    protected ContractAbi abi;

    public Contract(String address) {
        this.address = address;
    }

    public abstract String getContractType();

    protected abstract ContractAbi loadAbi();

    public String info() {
        return getInfo().toJSONString();
    }

    public String run(String method) {
        throw new IllegalArgumentException("METHOD NOT EXISTS");
    }

    public void setTronContractTrigger(TronContractTrigger trigger) {
        tronContractTrigger = trigger;
        abi = loadAbi();
    }

    public JSONObject getInfo() {
        JSONObject info = new JSONObject();
        info.put("address", getAddress());
        info.put("type", getContractType());
        return info;
    }
}
