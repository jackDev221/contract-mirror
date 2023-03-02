package org.tron.defi.contract_mirror.dao;

import com.alibaba.fastjson.JSON;
import lombok.Data;
import org.apache.kafka.clients.consumer.ConsumerRecord;

@Data
public class KafkaMessage<T> {
    private final long timestamp;
    private final T message;

    public KafkaMessage(T message) {
        this.message = message;
        timestamp = System.currentTimeMillis();
    }

    public KafkaMessage(ConsumerRecord<Long, String> record, Class<T> clz) {
        timestamp = record.timestamp();
        message = JSON.parseObject(record.value(), clz);
    }
}
