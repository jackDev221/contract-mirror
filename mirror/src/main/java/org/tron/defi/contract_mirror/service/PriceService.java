package org.tron.defi.contract_mirror.service;

import com.alibaba.fastjson.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
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
    RestClient priceCenter = new RestClient(null);

    public BigDecimal getPrice(IToken token) {
        Map<String, String> params = new HashMap<>(priceCenterConfig.getParams());
        UriComponentsBuilder uriBuilder = UriComponentsBuilder.newInstance()
                                                              .scheme("https")
                                                              .host(priceCenterConfig.getServer())
                                                              .path(priceCenterConfig.getUri());
        for (Map.Entry<String, String> entry : params.entrySet()) {
            uriBuilder.queryParam(entry.getKey(), entry.getValue());
        }
        uriBuilder.queryParam("symbol", token.getSymbol());
        try {
            String json = priceCenter.get(uriBuilder.build().toString());
            log.debug("http response {}", json);
            PriceResponse response = JSONObject.parseObject(json, PriceResponse.class);
            log.debug("PriceResponse {}", response);
            if (null == response.getStatus() || 0 != response.getStatus().getErrorCode()) {
                throw new RuntimeException("CANT GET TOKEN PRICE");
            }
            if (!response.getData().containsKey(token.getSymbol())) {
                return BigDecimal.ZERO;
            }
            return new BigDecimal(response.getData()
                                          .get(token.getSymbol())
                                          .getQuote()
                                          .getUsd()
                                          .getPrice());
        } catch (RuntimeException e) {
            e.printStackTrace();
            log.error(e.getMessage());
            throw new RuntimeException("CANT GET TOKEN PRICE");
        }
    }
}
