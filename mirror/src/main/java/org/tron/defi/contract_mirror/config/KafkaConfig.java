package org.tron.defi.contract_mirror.config;

import lombok.Data;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

@Data
@Component
@Configuration
@ConfigurationProperties(prefix = "kafka")
public class KafkaConfig {
    private String bootstrapServers;
    private String groupId;
    private long fallbackTime = 30 * 60 * 1000;
    private long maxLag = 60 * 1000;
    private List<String> consumerTopics = new ArrayList<>();
    private List<String> producerTopics = new ArrayList<>();

    private Properties consumerConfig;
    private Properties producerConfig;

    @PostConstruct
    public void initConfig() {
        if (!consumerTopics.isEmpty()) {
            consumerConfig = new Properties();
            consumerConfig.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
            consumerConfig.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
            consumerConfig.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
            consumerConfig.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG,
                               "org.apache.kafka.common.serialization.StringDeserializer");
            consumerConfig.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG,
                               "org.apache.kafka.common.serialization.StringDeserializer");
        }
        if (!producerTopics.isEmpty()) {
            producerConfig = new Properties();
            producerConfig.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
            producerConfig.put(ProducerConfig.ACKS_CONFIG, "all");
            producerConfig.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG,
                               "org.apache.kafka.common.serialization.StringDeserializer");
            producerConfig.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG,
                               "org.apache.kafka.common.serialization.StringDeserializer");
            producerConfig.put(ProducerConfig.MAX_BLOCK_MS_CONFIG, 500);
        }
    }
}
