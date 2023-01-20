package org.tron.sunio.contract_mirror.mirror.db;

import org.springframework.stereotype.Component;
import org.tron.sunio.contract_mirror.mirror.dao.Curve2PoolData;
import org.tron.sunio.contract_mirror.mirror.dao.Curve3PoolData;
import org.tron.sunio.contract_mirror.mirror.dao.Curve4PoolData;
import org.tron.sunio.contract_mirror.mirror.dao.PSMData;
import org.tron.sunio.contract_mirror.mirror.dao.SwapFactoryV1Data;
import org.tron.sunio.contract_mirror.mirror.dao.SwapFactoryV2Data;
import org.tron.sunio.contract_mirror.mirror.dao.SwapV1Data;
import org.tron.sunio.contract_mirror.mirror.dao.SwapV2PairData;
import org.tron.sunio.contract_mirror.mirror.db.cache.CacheHandler;

@Component
public class DbHandler implements IDbHandler {

    @Override
    public SwapV1Data querySwapV1Data(String address) {
        return CacheHandler.swapV1Cache.getIfPresent(address);
    }

    @Override
    public void updateSwapV1Data(SwapV1Data contractV1Data) {
        CacheHandler.swapV1Cache.put(contractV1Data.getAddress(), contractV1Data);
    }

    @Override
    public SwapFactoryV1Data querySwapFactoryV1Data(String address) {
        return CacheHandler.swapV1FactoryCache.getIfPresent(address);
    }

    @Override
    public void updateSwapFactoryV1Data(SwapFactoryV1Data factoryV1Data) {
        CacheHandler.swapV1FactoryCache.put(factoryV1Data.getAddress(), factoryV1Data);
    }

    @Override
    public SwapV2PairData querySwapV2PairData(String address) {
        return CacheHandler.swapV2PairCache.getIfPresent(address);
    }

    @Override
    public void updateSwapV2PairData(SwapV2PairData swapV2PairData) {
        CacheHandler.swapV2PairCache.put(swapV2PairData.getAddress(), swapV2PairData);
    }

    @Override
    public SwapFactoryV2Data querySwapFactoryV2Data(String address) {
        return CacheHandler.swapV2FactoryCache.getIfPresent(address);
    }

    @Override
    public void updateSwapFactoryV2Data(SwapFactoryV2Data factoryV2Data) {
        CacheHandler.swapV2FactoryCache.put(factoryV2Data.getAddress(), factoryV2Data);
    }

    @Override
    public Curve2PoolData queryCurve2PoolData(String address) {
        return CacheHandler.curve2PoolCache.getIfPresent(address);
    }

    @Override
    public void updateCurve2PoolData(Curve2PoolData curve2PoolData) {
        CacheHandler.curve2PoolCache.put(curve2PoolData.getAddress(), curve2PoolData);
    }

    @Override
    public Curve3PoolData queryCurve3PoolData(String address) {
        return CacheHandler.curve3PoolCache.getIfPresent(address);
    }

    @Override
    public void updateCurve3PoolData(Curve3PoolData curve3PoolData) {
        CacheHandler.curve3PoolCache.put(curve3PoolData.getAddress(), curve3PoolData);
    }

    @Override
    public Curve4PoolData queryCurve4PoolData(String address) {
        return CacheHandler.curve4PoolCache.getIfPresent(address);
    }

    @Override
    public void updateCurve4PoolData(Curve4PoolData curve4PoolData) {
        CacheHandler.curve4PoolCache.put(curve4PoolData.getAddress(), curve4PoolData);
    }

    @Override
    public PSMData queryPSMData(String address) {
        return CacheHandler.psmCache.getIfPresent(address);
    }

    @Override
    public void updatePSMData(PSMData psmData) {
        CacheHandler.psmCache.put(psmData.getAddress(), psmData);
    }
}
