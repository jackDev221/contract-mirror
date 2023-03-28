package org.tron.sunio.contract_mirror.mirror.router;

import cn.hutool.core.util.ObjectUtil;
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
            if (ObjectUtil.isNull(baseContract)) {
                return null;
            }
            baseContract = baseContract.copySelf();
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

    @Override
    public boolean isContractReady(String address) {
        if (containsContract(address)) {
            BaseContract baseContract = contractMaps.get(address);
            return baseContract.isReady();
        }
        BaseContract baseContract = getContract(address);
        if (ObjectUtil.isNull(baseContract)) {
            return false;
        }
        return baseContract.isReady();
    }

    public void init(Map<String, BaseContract> contractMaps) {
        this.contractMaps = contractMaps;
        pathCached.clear();
    }
}
