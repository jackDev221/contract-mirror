package org.tron.defi.contract_mirror.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Data
@Configuration
@Component
@ConfigurationProperties(prefix = "token-list")
public class TokenConfigList {
    private Map<String, String> tokens = new HashMap<>();
    private Map<String, String> wrapTokens = new HashMap<>();
}
