package org.tron.sunio.contract_mirror.mirror.servers;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.tron.sunio.contract_mirror.mirror.config.KafkaConfig;
import org.tron.sunio.contract_mirror.mirror.contracts.BaseContract;
import org.tron.sunio.contract_mirror.mirror.contracts.ContractFactoryManager;
import org.tron.sunio.contract_mirror.mirror.config.ContractsMirrorConfig;

import java.util.HashMap;

@Service
@Slf4j
@Data
public class ContractMirror implements InitializingBean {
    @Autowired
    private ContractFactoryManager contractFactoryManager;
    @Autowired
    private ContractsMirrorConfig config;

    @Autowired
    private KafkaConfig kafkaConfig;

    private HashMap<String, BaseContract> contractHashMap = new HashMap<>();

    @Override
    public void afterPropertiesSet() throws Exception {
        contractFactoryManager.initFactoryMap(config.getListContractFactory());
        contractFactoryManager.updateMirrorContracts();
    }
}
