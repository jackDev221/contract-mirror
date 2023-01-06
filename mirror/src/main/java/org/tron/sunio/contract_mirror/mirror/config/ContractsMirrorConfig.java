package org.tron.sunio.contract_mirror.mirror.config;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.tron.sunio.contract_mirror.mirror.contracts.ContractInfo;

import java.util.List;

@Data
@Slf4j
@Component
@ConfigurationProperties("contracts.mirror")
public class ContractsMirrorConfig {
    private String appName;
    List<ContractInfo> getListContractFactory() {
        return null;
    }
}
