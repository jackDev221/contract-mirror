package org.tron.sunio.contract_mirror.mirror.contracts.impl;

import org.tron.sunio.contract_mirror.mirror.chainHelper.IChainHelper;
import org.tron.sunio.contract_mirror.mirror.contracts.IContractsHelper;

import java.util.Map;

public class Assemble4Pool extends AssemblePool {
    private static final int BASE_COIN_SIZE = 4;
    private static final int COIN_SIZE = 2;

    public Assemble4Pool(String address, IChainHelper iChainHelper, IContractsHelper iContractsHelper, Map<String, String> sigMap) {
        super(address, COIN_SIZE, BASE_COIN_SIZE, iChainHelper, iContractsHelper, sigMap);
    }
}
