package org.tron.defi.contract_mirror.utils.kafka;

import lombok.Getter;
import org.apache.kafka.clients.consumer.ConsumerRebalanceListener;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.consumer.OffsetAndTimestamp;
import org.apache.kafka.common.TopicPartition;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class FallbackRebalanceListener implements ConsumerRebalanceListener {
    private final KafkaConsumer<Long, String> consumer;
    private final String topic;
    private final long fallbackTimeMs;
    @Getter
    private long initialEndOffset;

    public FallbackRebalanceListener(KafkaConsumer<Long, String> consumer,
                                     String topic,
                                     long fallbackTimeMs) {
        this.consumer = consumer;
        this.topic = topic;
        this.fallbackTimeMs = fallbackTimeMs;
    }

    @Override
    public void onPartitionsRevoked(Collection<TopicPartition> partitions) {

    }

    @Override
    public void onPartitionsAssigned(Collection<TopicPartition> partitions) {
        try {
            consumer.endOffsets(partitions).values().forEach(x -> {
                initialEndOffset = Math.max(initialEndOffset, x.longValue());
            });
            long startTime = System.currentTimeMillis() - fallbackTimeMs;
            Map<TopicPartition, Long> partitionTimes = new HashMap<>();
            for (TopicPartition topicPartition : partitions) {
                partitionTimes.put(new TopicPartition(topic, topicPartition.partition()),
                                   startTime);
            }
            Map<TopicPartition, OffsetAndTimestamp> partitionOffsets = consumer.offsetsForTimes(
                partitionTimes);
            for (Map.Entry<TopicPartition, OffsetAndTimestamp> partitionOffset :
                partitionOffsets.entrySet()) {
                consumer.seek(partitionOffset.getKey(), partitionOffset.getValue().offset());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
