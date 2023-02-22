package org.tron.defi.contract_mirror.config;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.tron.defi.contract_mirror.TestApplication;

import java.util.List;

@Slf4j
@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = TestApplication.class)
public class ContractConfigListTest {
    @Autowired
    private ContractConfigList contractConfigList;

    @Test
    void contractConfigListTest() {
        Assertions.assertNotNull(contractConfigList);
        List<ContractConfigList.ContractConfig> configList = contractConfigList.getContracts();
        log.info(configList.toString());
        Assertions.assertEquals(8, configList.size());
    }
}