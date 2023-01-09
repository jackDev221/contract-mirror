package org.tron.sunio.contract_mirror.mirror.config;

import cn.hutool.core.util.ObjectUtil;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.tron.sunio.contract_mirror.mirror.contracts.ContractInfo;
import org.tron.sunio.contract_mirror.mirror.enums.ContractType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Data
@Slf4j
@Component
@ConfigurationProperties("contract.mirror")
public class ContractsMirrorConfig {
    private String appName;

    private Map<String, String> factoryInfos = new HashMap<>();

    public List<ContractInfo> getListContractFactory() {
        List<ContractInfo> result = new ArrayList<>();
        if (ObjectUtil.isNull(factoryInfos)) {
            log.warn("ContractsMirrorConfig factoryInfos is null");
            return result;
        }
        factoryInfos.entrySet().stream().forEach(p ->
                result.add(new ContractInfo(p.getKey(), ContractType.find(p.getValue())))
        );
        return result;
    }
}
