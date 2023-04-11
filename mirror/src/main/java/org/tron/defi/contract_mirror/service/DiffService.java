package org.tron.defi.contract_mirror.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.tron.defi.contract.log.ContractLog;
import org.tron.defi.contract_mirror.config.KafkaConfig;
import org.tron.defi.contract_mirror.config.ServerConfig;
import org.tron.defi.contract_mirror.core.ContractManager;
import org.tron.defi.contract_mirror.core.consumer.DiffEventConsumer;
import org.tron.defi.contract_mirror.core.consumer.PendingEventConsumer;
import org.tron.defi.contract_mirror.core.consumer.SharedEventConsumer;
import org.tron.defi.contract_mirror.dao.KafkaMessage;

import java.util.Arrays;

@Slf4j
@Service
public class DiffService extends EventService {
    @Autowired
    public DiffService(KafkaConfig kafkaConfig,
                       ServerConfig serverConfig,
                       ContractManager contractManager) {
        super(kafkaConfig);
        serverConfig.setEventPoolConfig(null);  // force handle event synchronously
        setEventConsumers(Arrays.asList(new SharedEventConsumer(serverConfig,
                                                                contractManager,
                                                                new PendingEventConsumer(
                                                                    serverConfig,
                                                                    contractManager)),
                                        new DiffEventConsumer(contractManager)));
        contractManager.init();
        listen();
    }

    @Override
    protected void consume(KafkaMessage<ContractLog> message) {
        if (message.getOffset() < getInitialEndOffset()) {
            // wait to the latest offset
            return;
        }
        super.consume(message);
    }
}
