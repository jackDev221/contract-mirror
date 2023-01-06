package org.tron.sunio.contract_mirror.mirror.servers;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.tron.sunio.contract_mirror.mirror.contracts.ContractFactoryManager;
import org.tron.sunio.contract_mirror.mirror.config.ContractsMirrorConfig;

@Service
@Slf4j
public class ContractMirror implements InitializingBean {
    @Autowired
    private ContractFactoryManager contractFactoryManager;
    @Autowired
    private ContractsMirrorConfig config;

    @Override
    public void afterPropertiesSet() throws Exception {
        log.info("ContractsMirror Finish property set {}", this.config.getAppName());

        contractFactoryManager.initFactoryMap(null);
    }
}
