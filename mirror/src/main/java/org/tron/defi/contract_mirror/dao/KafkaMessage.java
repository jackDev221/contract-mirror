package org.tron.defi.contract_mirror.dao;

import com.alibaba.fastjson.JSON;
import lombok.Data;
import org.apache.kafka.clients.consumer.ConsumerRecord;

@Data
public class KafkaMessage<T> {
    private final long offset;
    private final long timestamp;
    private final T message;

    public KafkaMessage(T message) {
        offset = 0;
        timestamp = System.currentTimeMillis();
        this.message = message;
    }

    public KafkaMessage(ConsumerRecord<Long, String> record, Class<T> clz) {
        offset = record.offset();
        timestamp = record.timestamp();
        message = JSON.parseObject(record.value(), clz);
    }
}
