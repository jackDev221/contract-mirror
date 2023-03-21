package org.tron.sunio.contract_mirror.mirror.utils;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.tron.sunio.contract_mirror.mirror.contracts.BaseContract;
import org.tron.sunio.contract_mirror.mirror.contracts.IContractsHelper;

import java.util.HashMap;
import java.util.Map;

@Data
@NoArgsConstructor
public class ContractsHelper implements IContractsHelper {
    private long blockTime;
    private Map<String, BaseContract> contractMaps = new HashMap<>();

    @Override
    public long getBlockTime() {
        return blockTime;
    }

    @Override
    public BaseContract getContract(String address) {
        return contractMaps.get(address);
    }

    @Override
    public void addContract(BaseContract baseContract) {
        contractMaps.put(baseContract.getAddress(), baseContract);
    }

    @Override
    public boolean containsContract(String address) {
        return contractMaps.containsKey(address);
    }

    @Override
    public boolean isContractReady(String address) {
        if (!containsContract(address)) {
            return false;
        }
        BaseContract baseContract = contractMaps.get(address);
        return baseContract.isReady();
    }
}
