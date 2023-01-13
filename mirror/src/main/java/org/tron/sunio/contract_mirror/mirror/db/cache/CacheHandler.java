package org.tron.sunio.contract_mirror.mirror.db.cache;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import org.tron.sunio.contract_mirror.mirror.dao.SwapFactoryV1Data;
import org.tron.sunio.contract_mirror.mirror.dao.SwapV1Data;

public class CacheHandler {
    // max 16 * 32
    public final static Cache<String, SwapV1Data> v1Cache = CacheBuilder.newBuilder()
            .initialCapacity(2048)
            .concurrencyLevel(4)
            .build();

    // max 16 factory
    public final static Cache<String, SwapFactoryV1Data> v1FactoryCache = CacheBuilder.newBuilder()
            .initialCapacity(64)
            .concurrencyLevel(4)
            .build();
}
