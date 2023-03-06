package org.tron.defi.contract_mirror.application;


import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.tron.defi.contract_mirror.controller.ContractController;
import org.tron.defi.contract_mirror.service.ContractService;
import org.tron.defi.contract_mirror.service.SyncEventService;

@EnableConfigurationProperties
//@formatter:off
@ComponentScan(
    basePackages={"org.tron.defi.contract_mirror"},
    excludeFilters = {
        @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, value = Application.class),
        @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, value = ContractController.class),
        @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, value = ContractService.class),
        @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, value = SyncEventService.class),
    }
)
@SpringBootApplication(
    scanBasePackages = {"org.tron.defi.contract_mirror"},
    exclude = {
        DataSourceAutoConfiguration.class
    }
)
//@formatter:on
public class DiffApplication {
    public static void main(String[] args) {
        SpringApplication.run(DiffApplication.class, args);
    }
}
