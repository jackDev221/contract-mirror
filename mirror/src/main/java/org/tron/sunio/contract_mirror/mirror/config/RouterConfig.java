package org.tron.sunio.contract_mirror.mirror.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "router")
public class RouterConfig {
    private String priceUrl;
    private String baseTokens;//address
    private String baseTokenSymbols;//address
    private int maxHops;
    private int maxResultSize;
}
