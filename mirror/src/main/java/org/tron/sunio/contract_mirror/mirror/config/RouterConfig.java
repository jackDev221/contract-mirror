package org.tron.sunio.contract_mirror.mirror.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "router")
public class RouterConfig {
    public static final String ENV_MAIN = "main";
    public static final String ENV_NILE = "nile";
    public static final String ENV_SHASTA = "shasta";
    private String env;
    private String priceUrl;
    private String baseTokens;//address
    private String usdt;
    private String usdc;
    private String usdj;
    private String tusd;
    private int maxHops;
    private int maxResultSize;
}
