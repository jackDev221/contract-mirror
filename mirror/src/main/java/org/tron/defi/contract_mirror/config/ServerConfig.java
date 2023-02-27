package org.tron.defi.contract_mirror.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

@Data
@Component
@Configuration
@ConfigurationProperties(prefix = "server")
public class ServerConfig {
    private long syncPeriod;
    private ThreadPoolConfig eventPoolConfig;
    private ThreadPoolConfig pendingPoolConfig;

    @Data
    public static class ThreadPoolConfig {
        private int threadNum = 0;
    }
}
