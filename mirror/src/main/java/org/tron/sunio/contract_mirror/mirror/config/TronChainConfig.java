package org.tron.sunio.contract_mirror.mirror.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.tron.sunio.tronsdk.TronGrpcClient;

@Slf4j
@Configuration
public class TronChainConfig {
    private static final String NETWORK_NILE = "nile";
    private static final String NETWORK_SHASTA = "shasta";
    private static final String NETWORK_MAIN = "main";

    @Value("${contract.mirror.tron.network}")
    private String network;

    @Bean("mirrorTronClient")
    public TronGrpcClient createClient() {
        log.info("选择Tron 网络类型:{}", network);
        if (network.equalsIgnoreCase(NETWORK_SHASTA)) {
            return TronGrpcClient.ofShasta();
        }
        if (network.equalsIgnoreCase(NETWORK_NILE)) {
            return TronGrpcClient.ofNile();
        }
        if (network.equalsIgnoreCase(NETWORK_MAIN)) {
            return TronGrpcClient.ofMainNet();
        }
        return TronGrpcClient.ofShasta();
    }
}
