package org.tron.defi.contract_mirror.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.tron.defi.contract_mirror.config.KafkaConfig;
import org.tron.defi.contract_mirror.config.ServerConfig;
import org.tron.defi.contract_mirror.core.ContractManager;
import org.tron.defi.contract_mirror.core.consumer.PendingEventConsumer;
import org.tron.defi.contract_mirror.core.consumer.SharedEventConsumer;

import java.util.Collections;

@Service
public class SyncEventService extends EventService {
    @Autowired
    public SyncEventService(KafkaConfig kafkaConfig,
                            ServerConfig serverConfig,
                            ContractManager contractManager) {
        super(kafkaConfig);
        setEventConsumers(Collections.singletonList(new SharedEventConsumer(serverConfig,
                                                                            contractManager,
                                                                            new PendingEventConsumer(
                                                                                serverConfig,
                                                                                contractManager))));
    }
}
