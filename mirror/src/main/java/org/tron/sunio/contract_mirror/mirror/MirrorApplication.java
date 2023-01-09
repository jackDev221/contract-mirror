package org.tron.sunio.contract_mirror.mirror;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@EnableConfigurationProperties
@SpringBootApplication(scanBasePackages = "org.tron.sunio")
public class MirrorApplication {

    public static void main(String[] args) {
        SpringApplication.run(MirrorApplication.class, args);
    }

}
