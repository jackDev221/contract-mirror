package org.tron.defi.contract_mirror.service;

import com.alibaba.fastjson.JSONObject;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponentsBuilder;
import org.tron.defi.contract_mirror.config.PriceCenterConfig;
import org.tron.defi.contract_mirror.dto.legacy.PriceResponse;
import org.tron.defi.contract_mirror.utils.RestClient;

import javax.annotation.PostConstruct;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class PriceService {
    @Autowired
    private PriceCenterConfig priceCenterConfig;
    private RestClient priceCenter = new RestClient(null);
    private Cache<String, BigDecimal> priceCache;

    public BigDecimal getPrice(String symbolOrAddress) {
        BigDecimal price = getPriceFromCache(symbolOrAddress);
        if (null != price) {
            return price;
        }
        Map<String, String> params = new HashMap<>(priceCenterConfig.getParams());
        UriComponentsBuilder uriBuilder = UriComponentsBuilder.newInstance()
                                                              .scheme("https")
                                                              .host(priceCenterConfig.getServer())
                                                              .path(priceCenterConfig.getUri());
        for (Map.Entry<String, String> entry : params.entrySet()) {
            uriBuilder.queryParam(entry.getKey(), entry.getValue());
        }
        uriBuilder.queryParam("symbol", symbolOrAddress);
        try {
            String json = priceCenter.get(uriBuilder.build().toString());
            log.debug("http response {}", json);
            PriceResponse response = JSONObject.parseObject(json, PriceResponse.class);
            log.debug("PriceResponse {}", response);
            if (null == response.getStatus() || 0 != response.getStatus().getErrorCode()) {
                throw new RuntimeException("CANT GET TOKEN PRICE");
            }
            if (!response.getData().containsKey(symbolOrAddress)) {
                return BigDecimal.ZERO;
            }
            price = new BigDecimal(response.getData()
                                           .get(symbolOrAddress)
                                           .getQuote()
                                           .getUsd()
                                           .getPrice());
            updateCache(symbolOrAddress, price);
            return price;
        } catch (RuntimeException e) {
            e.printStackTrace();
            log.error(e.getMessage());
            throw new RuntimeException("CANT GET TOKEN PRICE");
        }
    }

    public BigDecimal getPriceFromCache(String symbolOrAddress) {
        if (null == priceCache) {
            return null;
        }
        return priceCache.getIfPresent(symbolOrAddress);
    }

    @PostConstruct
    public void initCache() {
        PriceCenterConfig.CacheConfig cacheConfig = priceCenterConfig.getCacheConfig();
        if (null == cacheConfig) {
            return;
        }
        priceCache = CacheBuilder.newBuilder()
                                 .concurrencyLevel(cacheConfig.getConcurrencyLevel())
                                 .initialCapacity(30000)
                                 .maximumSize(cacheConfig.getMaxCacheSize())
                                 .expireAfterWrite(cacheConfig.getExpireTime(), TimeUnit.SECONDS)
                                 .build();
    }

    public void updateCache(String symbolOrAddress, BigDecimal price) {
        if (null == priceCache) {
            return;
        }
        priceCache.put(symbolOrAddress, price);
    }
}
