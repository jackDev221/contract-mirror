package org.tron.defi.contract_mirror.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;
import org.tron.defi.contract_mirror.utils.RestClient;

import java.util.HashMap;
import java.util.Map;


@Data
@Component
@Configuration
@ConfigurationProperties(prefix = "price-center")
public class PriceCenterConfig {
    private String server;
    private String uri;
    private Map<String, String> params = new HashMap<>();

    @Bean(name = "priceCenter")
    public RestClient getRestClient() {
        return new RestClient(server, null);
    }
}
