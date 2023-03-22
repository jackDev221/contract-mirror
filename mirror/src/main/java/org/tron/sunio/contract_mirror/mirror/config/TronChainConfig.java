package org.tron.sunio.contract_mirror.mirror.config;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.tron.sunio.tronsdk.TronGrpcClient;

@Slf4j
@Data
@Configuration
@ConfigurationProperties(prefix = "tronconfig")
public class TronChainConfig {
    private String grpcEndpoint;
    private String grpcEndpointSolidity;

    @Bean("mirrorTronClient")
    public TronGrpcClient createClient() {
        log.info("选择Tron url:{} / {}", grpcEndpoint, grpcEndpointSolidity);
        return TronGrpcClient.ofNetwork(grpcEndpoint, grpcEndpointSolidity);

    }
}
