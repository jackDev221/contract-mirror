package org.tron.sunio.contract_mirror.mirror.contracts.impl;

import org.tron.sunio.contract_mirror.mirror.chainHelper.IChainHelper;
import org.tron.sunio.contract_mirror.mirror.contracts.IContractsHelper;
import org.tron.sunio.contract_mirror.mirror.enums.ContractType;

import java.util.Map;

public class Assemble3Pool extends AssemblePool {
    private static final int BASE_COIN_SIZE = 2;
    private static final int COIN_SIZE = 2;

    public Assemble3Pool(String address, IChainHelper iChainHelper, IContractsHelper iContractsHelper, Map<String, String> sigMap) {
        super(address, ContractType.CONTRACT_ASSEMBLE_3POOL, COIN_SIZE, BASE_COIN_SIZE, iChainHelper, iContractsHelper, sigMap);
    }
}
