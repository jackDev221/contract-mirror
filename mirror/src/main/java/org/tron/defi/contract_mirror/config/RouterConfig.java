package org.tron.defi.contract_mirror.config;


import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.Set;

@Data
@Component
@Configuration
@ConfigurationProperties(prefix = "router")
public class RouterConfig {
    private String strategy = "DEFAULT";
    private int maxCost = 3;
    private int topN = 3;
    private Set<String> tokenWhiteList = new HashSet<>();
    private Set<String> poolBlackList = new HashSet<>();
}
