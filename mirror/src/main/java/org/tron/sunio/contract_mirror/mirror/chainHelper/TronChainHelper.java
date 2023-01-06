package org.tron.sunio.contract_mirror.mirror.chainHelper;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.tron.sunio.tronsdk.TronGrpcClient;

@Component
public class TronChainHelper implements IChainHelper {
    @Autowired
    @Qualifier("mirrorTronClient")
    private TronGrpcClient tronGrpcClient;

    @Override
    public long blockNumber() {
        return 0;
    }
}
