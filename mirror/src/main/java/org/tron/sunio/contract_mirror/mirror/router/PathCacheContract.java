package org.tron.sunio.contract_mirror.mirror.router;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.tron.sunio.contract_mirror.mirror.contracts.BaseContract;
import org.tron.sunio.contract_mirror.mirror.contracts.IContractsHelper;
import org.tron.sunio.contract_mirror.mirror.contracts.impl.AbstractCurve;
import org.tron.sunio.contract_mirror.mirror.enums.ContractType;

import java.util.HashMap;
import java.util.Map;

@NoArgsConstructor
@Data
public class PathCacheContract implements IContractsHelper {
    private Map<String, BaseContract> pathCached = new HashMap<>(10);
    private Map<String, BaseContract> contractMaps;

    @Override
    public long getBlockTime() {
        return 0;
    }

    @Override
    public BaseContract getContract(String address) {
        if (this.pathCached.containsKey(address)) {
            return pathCached.get(address);
        } else {
            BaseContract baseContract = contractMaps.get(address);
            if (baseContract.getType() == ContractType.CONTRACT_CURVE_2POOL
                    || baseContract.getType() == ContractType.CONTRACT_CURVE_3POOL
                    || baseContract.getType() == ContractType.CONTRACT_CURVE_4POOL
                    || baseContract.getType() == ContractType.STABLE_SWAP_POOL
            ) {
                baseContract = ((AbstractCurve) baseContract).copySelf();
            }
            pathCached.put(address, baseContract);
            return baseContract;
        }
    }

    @Override
    public void addContract(BaseContract baseContract) {
        pathCached.put(baseContract.getAddress(), baseContract);
    }

    @Override
    public boolean containsContract(String address) {
        return pathCached.containsKey(address);
    }

    public void init(Map<String, BaseContract> contractMaps) {
        this.contractMaps = contractMaps;
        pathCached.clear();
    }
}
