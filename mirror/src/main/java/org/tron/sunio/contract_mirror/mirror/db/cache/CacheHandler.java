package org.tron.sunio.contract_mirror.mirror.db.cache;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import org.tron.sunio.contract_mirror.mirror.dao.Curve2PoolData;
import org.tron.sunio.contract_mirror.mirror.dao.Curve3PoolData;
import org.tron.sunio.contract_mirror.mirror.dao.SwapFactoryV1Data;
import org.tron.sunio.contract_mirror.mirror.dao.SwapFactoryV2Data;
import org.tron.sunio.contract_mirror.mirror.dao.SwapV1Data;
import org.tron.sunio.contract_mirror.mirror.dao.SwapV2PairData;

public class CacheHandler {
    // max 2048 * 64
    public final static Cache<String, SwapV1Data> swapV1Cache = CacheBuilder.newBuilder()
            .initialCapacity(2048 * 64)
            .concurrencyLevel(4)
            .build();

    // max 16 factory
    public final static Cache<String, SwapFactoryV1Data> swapV1FactoryCache = CacheBuilder.newBuilder()
            .initialCapacity(64)
            .concurrencyLevel(4)
            .build();

    // max 2048 * 64
    public final static Cache<String, SwapV2PairData> swapV2PairCache = CacheBuilder.newBuilder()
            .initialCapacity(2048 * 64)
            .concurrencyLevel(4)
            .build();

    // max 16 factory
    public final static Cache<String, SwapFactoryV2Data> swapV2FactoryCache = CacheBuilder.newBuilder()
            .initialCapacity(64)
            .concurrencyLevel(4)
            .build();

    // max 2048 * 64
    public final static Cache<String, Curve2PoolData> curve2PoolCache = CacheBuilder.newBuilder()
            .initialCapacity(2048 * 64)
            .concurrencyLevel(4)
            .build();


    // max 2048 * 64
    public final static Cache<String, Curve3PoolData> curve3PoolCache = CacheBuilder.newBuilder()
            .initialCapacity(2048 * 64)
            .concurrencyLevel(4)
            .build();
}
