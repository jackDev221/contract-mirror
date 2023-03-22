package org.tron.defi.contract_mirror.config;


import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.HashSet;
import java.util.Set;

@Data
@Component
@Configuration
@ConfigurationProperties(prefix = "router")
public class RouterConfig {
    @Autowired
    private TokenConfigList tokenConfigList;
    private String strategy = "DEFAULT";
    private int maxCost = 3;
    private int topN = 3;
    private Set<String> tokenWhiteList = new HashSet<>();
    private Set<String> poolBlackList = new HashSet<>();

    @PostConstruct
    void initTokenWhiteList() {
        Set<String> tokenList = new HashSet<>(tokenWhiteList.size());
        for (String name : tokenWhiteList) {
            String address = tokenConfigList.getTokens().getOrDefault(name, null);
            if (null == address) {
                continue;
            }
            tokenList.add(address);
        }
        tokenWhiteList = tokenList;
    }
}
