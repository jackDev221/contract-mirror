package org.tron.defi.contract_mirror.service;

import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.tron.defi.contract.log.ContractLog;
import org.tron.defi.contract_mirror.config.KafkaConfig;
import org.tron.defi.contract_mirror.core.consumer.IEventConsumer;
import org.tron.defi.contract_mirror.dao.KafkaMessage;
import org.tron.defi.contract_mirror.utils.kafka.FallbackRebalanceListener;

import java.time.Duration;
import java.util.Collections;
import java.util.List;

@Slf4j
public class EventService {
    protected final KafkaConfig kafkaConfig;
    private final EventListenThread listenThread = new EventListenThread();
    @Setter
    private List<IEventConsumer> eventConsumers;
    private KafkaConsumer<Long, String> kafkaConsumer;
    private FallbackRebalanceListener fallbackRebalanceListener;

    public EventService(KafkaConfig kafkaConfig) {
        this.kafkaConfig = kafkaConfig;
        initConsumer();
    }

    public long getInitialEndOffset() {
        return fallbackRebalanceListener.getInitialEndOffset();
    }

    public void listen() {
        listenThread.start();
    }

    protected void consume(KafkaMessage<ContractLog> message) {
        eventConsumers.forEach(consumer -> consumer.consume(message));
    }

    private void initConsumer() {
        if (null == kafkaConfig.getConsumerConfig()) {
            return;
        }
        kafkaConsumer = new KafkaConsumer<>(kafkaConfig.getConsumerConfig());
        String topic = kafkaConfig.getConsumerTopics().get(0);
        // initialize kafka start timestamp to now - fallbackTime
        fallbackRebalanceListener = new FallbackRebalanceListener(kafkaConsumer,
                                                                  topic,
                                                                  kafkaConfig.getFallbackTime());
        kafkaConsumer.subscribe(Collections.singleton(topic), fallbackRebalanceListener);
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
