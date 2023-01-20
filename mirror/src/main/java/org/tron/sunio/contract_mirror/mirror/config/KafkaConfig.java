package org.tron.sunio.contract_mirror.mirror.config;

import lombok.Data;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.Arrays;
import java.util.List;
import java.util.Properties;

@Data
@Configuration
@ConfigurationProperties(prefix = "kafka")
public class KafkaConfig {
    @Value("${servers:tctest-kafka-t2-1.tc-jp1.huobiidc.com:9092}")
    private String servers;
    @Value("${groupId:test}")
    private String groupId;
    @Value("${consumerTopics:[test_topicContractLog]}")
    private String[] consumerTopics;
    @Value("${producerTopics:test_topicContractLog}")
    private String producerTopics;

    public Properties consumerConfig() {
        Properties config = new Properties();
        config.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
        config.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        config.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, servers);
        //config.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        config.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "latest");
        config.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringDeserializer");
        config.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringDeserializer");
        return config;
    }

    public Properties producerConfig() {
        Properties config = new Properties();
        config.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, servers);
        config.put(ProducerConfig.ACKS_CONFIG, "all");
        config.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringDeserializer");
        config.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringDeserializer");
        config.put(ProducerConfig.MAX_BLOCK_MS_CONFIG, 500);
        return config;
    }

    public List<String> getTopics() {
        return Arrays.asList(consumerTopics);
    }
}
