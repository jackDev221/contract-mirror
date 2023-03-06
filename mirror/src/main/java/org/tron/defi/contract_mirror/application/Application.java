package org.tron.defi.contract_mirror.application;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.tron.defi.contract_mirror.service.DiffService;

@EnableConfigurationProperties
//@formatter:off
@ComponentScan(
    basePackages={"org.tron.defi.contract_mirror"},
    excludeFilters = {
        @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, value = DiffApplication.class),
        @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, value = DiffService.class),
    }
)
@SpringBootApplication(
    scanBasePackages = {"org.tron.defi.contract_mirror"},
    exclude = {
        DataSourceAutoConfiguration.class
    }
)
//@formatter:on
public class Application {
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}
