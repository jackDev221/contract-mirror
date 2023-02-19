package org.tron.defi.contract_mirror.core.pool;


import org.tron.defi.contract.abi.ContractAbi;
import org.tron.defi.contract.abi.pool.SunswapV1Abi;
import org.tron.defi.contract.log.ContractLog;
import org.tron.defi.contract_mirror.dao.KafkaMessage;

public class SunswapV1Pool extends Pool {
    public SunswapV1Pool(String address) {
        super(address);
        this.type = PoolType.SUNSWAP_V1;
    }

    @Override
    protected ContractAbi loadAbi() {
        return tronContractTrigger.contractAt(SunswapV1Abi.class, getAddress());
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
