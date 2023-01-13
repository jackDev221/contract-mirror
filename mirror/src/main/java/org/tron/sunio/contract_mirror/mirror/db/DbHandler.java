package org.tron.sunio.contract_mirror.mirror.db;

import org.springframework.stereotype.Component;
import org.tron.sunio.contract_mirror.mirror.dao.ContractFactoryV1Data;
import org.tron.sunio.contract_mirror.mirror.dao.ContractV1Data;
import org.tron.sunio.contract_mirror.mirror.db.cache.CacheHandler;

@Component
public class DbHandler implements IDbHandler {

    @Override
    public ContractV1Data queryContractV1Data(String address) {
        return CacheHandler.v1Cache.getIfPresent(address);
    }

    @Override
    public void updateContractV1Data(ContractV1Data contractV1Data) {
        CacheHandler.v1Cache.put(contractV1Data.getAddress(), contractV1Data);
    }

    @Override
    public ContractFactoryV1Data queryContractFactoryV1Data(String address) {
        return CacheHandler.v1FactoryCache.getIfPresent(address);
    }

    @Override
    public void updateContractFactoryV1Data(ContractFactoryV1Data factoryV1Data) {
        CacheHandler.v1FactoryCache.put(factoryV1Data.getAddress(), factoryV1Data);
    }
}
