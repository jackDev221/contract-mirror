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
}
