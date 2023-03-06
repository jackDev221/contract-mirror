package org.tron.defi.contract_mirror.service;

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
    private EventListenThread listenThread;
    private KafkaConsumer<Long, String> kafkaConsumer;
    private List<IEventConsumer> eventConsumers;

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

    public void listen() {
        listenThread = new EventListenThread();
        listenThread.start();
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
                            KafkaMessage<ContractLog> message = new KafkaMessage<>(record,
                                                                                   ContractLog.class);
                            eventConsumers.forEach(consumer -> consumer.consume(message));
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }


}
