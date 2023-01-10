package org.tron.sunio.contract_mirror.mirror.cache;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import org.springframework.stereotype.Component;
import org.tron.sunio.contract_mirror.mirror.dao.ContractFactoryV1Data;
import org.tron.sunio.contract_mirror.mirror.dao.ContractV1Data;


@Component
public class CacheHandler {

    // max 16 * 32
    public final static Cache<String, ContractV1Data> v1Cache = CacheBuilder.newBuilder()
            .initialCapacity(2048)
            .concurrencyLevel(4)
            .build();

    // max 16 factory
    public final static Cache<String, ContractFactoryV1Data> v1FactoryCache = CacheBuilder.newBuilder()
            .initialCapacity(64)
            .concurrencyLevel(4)
            .build();
}
