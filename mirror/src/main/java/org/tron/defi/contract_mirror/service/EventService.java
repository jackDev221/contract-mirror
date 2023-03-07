package org.tron.defi.contract_mirror.service;

import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.TopicPartition;
import org.tron.defi.contract.log.ContractLog;
import org.tron.defi.contract_mirror.config.KafkaConfig;
import org.tron.defi.contract_mirror.core.consumer.IEventConsumer;
import org.tron.defi.contract_mirror.dao.KafkaMessage;
import org.tron.defi.contract_mirror.utils.kafka.FallbackRebalanceListener;

import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
public class EventService {
    protected final KafkaConfig kafkaConfig;
    protected KafkaConsumer<Long, String> kafkaConsumer;
    protected List<IEventConsumer> eventConsumers;
    protected long consumerEndOffset;
    private EventListenThread listenThread;

    public EventService(KafkaConfig kafkaConfig) {
        this.kafkaConfig = kafkaConfig;
    }

    public void init(List<IEventConsumer> eventConsumers) {
        this.eventConsumers = eventConsumers;
        if (null != kafkaConfig.getConsumerConfig()) {
            kafkaConsumer = new KafkaConsumer<>(kafkaConfig.getConsumerConfig());
            String topic = kafkaConfig.getConsumerTopics().get(0);
            // initialize kafka start timestamp to now - maxLag
            FallbackRebalanceListener fallbackRebalanceListener = new FallbackRebalanceListener(
                kafkaConsumer,
                topic,
                kafkaConfig.getMaxLag());
            kafkaConsumer.subscribe(Collections.singleton(topic), fallbackRebalanceListener);
        }
    }

    public void initConsumerEndOffset() {
        String topic = kafkaConfig.getConsumerTopics().get(0);
        List<TopicPartition> topicPartitions = kafkaConsumer.partitionsFor(topic)
                                                            .stream()
                                                            .map(x -> {
                                                                return new TopicPartition(x.topic(),
                                                                                          x.partition());
                                                            })
                                                            .collect(Collectors.toList());
        kafkaConsumer.endOffsets(topicPartitions).values().forEach(x -> {
            consumerEndOffset = Math.max(consumerEndOffset, x.longValue());
        });
        log.info("{} latest offset {}", topic, consumerEndOffset);
    }

    public void listen() {
        listenThread = new EventListenThread();
        initConsumerEndOffset();
        listenThread.start();
    }

    protected void consume(KafkaMessage<ContractLog> message) {
        eventConsumers.forEach(consumer -> consumer.consume(message));
    }

    private class EventListenThread extends Thread {
        @Override
        public void run() {
            while (true) {
                try {
                    while (true) {
                        ConsumerRecords<Long, String> consumerRecords
                            = kafkaConsumer.poll(Duration.ofMillis(kafkaConfig.getMaxLag()));
                        if (0 == consumerRecords.count()) {
                            continue;
                        }
                        for (ConsumerRecord<Long, String> record : consumerRecords) {
                            log.debug(record.value());
                            consume(new KafkaMessage<>(record, ContractLog.class));
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }


}
