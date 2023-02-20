package org.tron.sunio.contract_mirror.mirror.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "router")
public class RouterConfig {
    private String baseTokens;
    private String usdt;
    private String usdc;
    private String usdj;
    private String tusd;
    private int maxHops;
}
