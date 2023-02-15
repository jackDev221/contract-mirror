package org.tron.defi.contract_mirror.dao;

import com.alibaba.fastjson.JSON;
import lombok.Data;
import org.apache.kafka.clients.consumer.ConsumerRecord;

@Data
public class KafkaMessage<T> {
    private long timestamp;
    private T message;

    public KafkaMessage(ConsumerRecord<Long, String> record) {
        timestamp = record.timestamp();
        message = JSON.parseObject(record.value(), (Class<T>) message.getClass());
    }
}
