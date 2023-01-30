package org.tron.sunio.contract_mirror.mirror.pool;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.tron.sunio.contract_mirror.mirror.enums.ContractType;
import org.tron.sunio.contract_mirror.mirror.pool.process.IProcessor;
import org.tron.sunio.contract_mirror.mirror.pool.process.impl.InvalidProcessor;
import org.tron.sunio.contract_mirror.mirror.pool.process.impl.SwapV1FactoryExcProcess;
import org.tron.sunio.contract_mirror.mirror.pool.process.impl.SwapV2FactoryExcProcess;

import javax.annotation.PostConstruct;
import java.util.HashMap;
import java.util.Map;


@Component
public class ProcessInitializer {
    @Autowired
    private SwapV1FactoryExcProcess swapV1FactoryExcProcess;

    @Autowired
    private SwapV2FactoryExcProcess swapV2FactoryExcProcess;

    private final Map<String, IProcessor> processors = new HashMap<>();
    private final InvalidProcessor defaultProcessor = new InvalidProcessor();

    @PostConstruct
    public void init() {
        registerProcessor(ContractType.SWAP_FACTORY_V1.getDesc(), swapV1FactoryExcProcess);
        registerProcessor(ContractType.SWAP_FACTORY_V2.getDesc(), swapV2FactoryExcProcess);
    }

    private void registerProcessor(String key, IProcessor processor) {
        processors.put(key, processor);
    }

    public IProcessor getProcessor(String topic) {
        return processors.getOrDefault(topic, defaultProcessor);
    }
}
