package org.tron.defi.contract_mirror.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;
import org.tron.defi.contract_mirror.core.ContractType;

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
        private String name;
        private String address;
        private ContractType type;
        private String polyAddress;
        private CurveConfig curveConfig;
    }

    @Data
    public static class CurveConfig {
        private int feeIndex;
        private int feeDenominator;
        private int precision;
        private int precisionA;
        private List<Integer> rates;
    }
}
