package org.tron.defi.contract_mirror;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.tron.defi.contract_mirror.application.Application;
import org.tron.defi.contract_mirror.controller.ContractController;
import org.tron.defi.contract_mirror.service.ContractService;
import org.tron.defi.contract_mirror.service.EventService;

@EnableConfigurationProperties
//@formatter:off
@ComponentScan(excludeFilters = {
    @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, value = Application.class),
    @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, value = ContractController.class),
    @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, value = ContractService.class),
    @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, value = EventService.class)
})
//@formatter:on
@SpringBootApplication(scanBasePackages = {"org.tron.defi.contract_mirror"}, exclude = {
    DataSourceAutoConfiguration.class})
public class TestApplication {
    public static void main(String[] args) {
        SpringApplication.run(TestApplication.class, args);
    }
}
