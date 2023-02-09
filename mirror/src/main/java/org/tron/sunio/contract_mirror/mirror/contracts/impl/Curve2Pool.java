package org.tron.sunio.contract_mirror.mirror.contracts.impl;

import org.tron.sunio.contract_mirror.mirror.chainHelper.IChainHelper;
import org.tron.sunio.contract_mirror.mirror.contracts.IContractsHelper;
import org.tron.sunio.contract_mirror.mirror.enums.ContractType;

import java.util.Map;

public class Curve2Pool extends CurveBasePool {
    public Curve2Pool(String address, IChainHelper iChainHelper, IContractsHelper iContractsHelper, int coinsCount, int feeIndex, Map<String, String> sigMap) {
        super(address, ContractType.CONTRACT_CURVE_2POOL, iChainHelper, iContractsHelper, coinsCount, feeIndex, sigMap);
    }
}
