package org.tron.sunio.contract_mirror.mirror.db;

import org.springframework.stereotype.Component;
import org.tron.sunio.contract_mirror.mirror.dao.SwapFactoryV1Data;
import org.tron.sunio.contract_mirror.mirror.dao.SwapV1Data;
import org.tron.sunio.contract_mirror.mirror.db.cache.CacheHandler;

@Component
public class DbHandler implements IDbHandler {

    @Override
    public SwapV1Data queryContractV1Data(String address) {
        return CacheHandler.v1Cache.getIfPresent(address);
    }

    @Override
    public void updateContractV1Data(SwapV1Data contractV1Data) {
        CacheHandler.v1Cache.put(contractV1Data.getAddress(), contractV1Data);
    }

    @Override
    public SwapFactoryV1Data queryContractFactoryV1Data(String address) {
        return CacheHandler.v1FactoryCache.getIfPresent(address);
    }

    @Override
    public void updateContractFactoryV1Data(SwapFactoryV1Data factoryV1Data) {
        CacheHandler.v1FactoryCache.put(factoryV1Data.getAddress(), factoryV1Data);
    }
}
