package org.tron.defi.contract_mirror.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;
import org.tron.defi.contract_mirror.common.ContractType;

import java.util.ArrayList;
import java.util.List;

@Data
@Configuration
@Component
@ConfigurationProperties(prefix = "contract-list")
public class ContractConfigList {
    private List<ContractConfig> contracts = new ArrayList<>();

    @Data
    public static class ContractConfig {
        private String address;
        private ContractType type;
        private String polyAddress;
    }
}

