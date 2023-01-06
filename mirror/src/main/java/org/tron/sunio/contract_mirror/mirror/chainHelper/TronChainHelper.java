package org.tron.sunio.contract_mirror.mirror.chainHelper;

import org.springframework.stereotype.Component;

@Component
public class TronChainHelper implements IChainHelper{
    @Override
    public long blockNumber() {
        return 0;
    }
}
