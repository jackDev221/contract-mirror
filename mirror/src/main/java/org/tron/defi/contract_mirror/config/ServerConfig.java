package org.tron.defi.contract_mirror.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;

@Data
@Component
@Configuration
@ConfigurationProperties(prefix = "server")
public class ServerConfig {
    private ThreadPoolConfig eventPoolConfig;
    private ThreadPoolConfig pendingPoolConfig;

    private static ThreadPoolTaskExecutor createThreadPool(ThreadPoolConfig threadPoolConfig) {
        ThreadPoolTaskExecutor poolTaskExecutor = new ThreadPoolTaskExecutor();
        if (null == threadPoolConfig) {
            return poolTaskExecutor;
        }
        if (threadPoolConfig.getThreadNum() > 0) {
            poolTaskExecutor.setCorePoolSize(threadPoolConfig.getThreadNum());
        }
        return poolTaskExecutor;
    }

    @Data
    public static class ThreadPoolConfig {
        private int threadNum = 0;
    }
}
