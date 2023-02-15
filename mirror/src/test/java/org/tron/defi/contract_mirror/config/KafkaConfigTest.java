package org.tron.defi.contract_mirror.config;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.tron.defi.contract_mirror.TestApplication;

@Slf4j
@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = TestApplication.class)
public class KafkaConfigTest {
    @Autowired
    private KafkaConfig kafkaConfig;

    @Test
    void kafkaConfigTest() {
        Assertions.assertNotNull(kafkaConfig);
        log.info(kafkaConfig.toString());
        Assertions.assertEquals(kafkaConfig.getConsumerConfig() == null,
                                kafkaConfig.getConsumerTopics().isEmpty());
        Assertions.assertEquals(kafkaConfig.getProducerConfig() == null,
                                kafkaConfig.getProducerTopics().isEmpty());
    }
}
