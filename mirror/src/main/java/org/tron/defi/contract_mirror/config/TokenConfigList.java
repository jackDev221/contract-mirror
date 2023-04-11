package org.tron.defi.contract_mirror.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.HashMap;
import java.util.Map;

@Data
@Configuration
@Component
@ConfigurationProperties(prefix = "token-list")
public class TokenConfigList {
    private Map<String, String> tokens = new HashMap<>();
    private Map<String, String> wrapTokens = new HashMap<>();

    @PostConstruct
    void initWrapTokenAddressMapping() {
        Map<String, String> mapping = new HashMap<>(wrapTokens);
        for (Map.Entry<String, String> entry : wrapTokens.entrySet()) {
            String address0 = tokens.getOrDefault(entry.getKey(), null);
            String address1 = tokens.getOrDefault(entry.getValue(), null);
            if (null == address0 || null == address1) {
                continue;
            }
            mapping.put(address0, address1);
        }
        wrapTokens = mapping;
    }
}
