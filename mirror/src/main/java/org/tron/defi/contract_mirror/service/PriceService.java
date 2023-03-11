package org.tron.defi.contract_mirror.service;

import com.alibaba.fastjson.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriBuilder;
import org.springframework.web.util.UriComponentsBuilder;
import org.tron.defi.contract_mirror.config.PriceCenterConfig;
import org.tron.defi.contract_mirror.core.token.IToken;
import org.tron.defi.contract_mirror.dto.legacy.PriceResponse;
import org.tron.defi.contract_mirror.utils.RestClient;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
public class PriceService {
    @Autowired
    PriceCenterConfig priceCenterConfig;
    @Autowired
    RestClient priceCenter;

    public BigDecimal getPrice(IToken token) {
        Map<String, String> params = new HashMap<>(priceCenterConfig.getParams());
        params.put("symbol", token.getSymbol());
        UriBuilder uriBuilder = UriComponentsBuilder.fromUriString(priceCenterConfig.getUri());
        try {
            PriceResponse response = JSONObject.parseObject(priceCenter.get(uriBuilder.build(params)
                                                                                      .toString()),
                                                            PriceResponse.class);
            if (null == response.getStatus() || 0 != response.getStatus().getErrorCode()) {
                log.error("response {}", response);
                throw new RuntimeException("CANT GET TOKEN PRICE");
            }
            if (!response.getData().containsKey(token.getSymbol())) {
                return BigDecimal.ZERO;
            }
            return new BigDecimal(response.getData().get(token.getSymbol()).getUsd().getPrice());
        } catch (RuntimeException e) {
            log.error(e.getMessage());
            throw new RuntimeException("CANT GET TOKEN PRICE");
        }
    }
}
