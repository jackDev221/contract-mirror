package org.tron.defi.contract_mirror.core.pool;


import org.tron.defi.contract_mirror.dao.ContractLog;
import org.tron.defi.contract_mirror.dao.KafkaMessage;

public class SunswapV1Pool extends Pool {
    public SunswapV1Pool(String address) {
        super(address);
        this.type = PoolType.SUNSWAP_V1;
    }

    @Override
    public boolean init() {
        return tokens.size() == 2;
    }

    @Override
    public void sync() {
        // TODO finish implementation
    }

    @Override
    public void onEvent(KafkaMessage<ContractLog> kafkaMessage) {
        // TODO finish implementation
    }
}
