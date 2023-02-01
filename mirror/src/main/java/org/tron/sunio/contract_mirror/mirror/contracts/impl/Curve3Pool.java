package org.tron.sunio.contract_mirror.mirror.contracts.impl;

import lombok.extern.slf4j.Slf4j;
import org.tron.sunio.contract_mirror.mirror.chainHelper.IChainHelper;
import org.tron.sunio.contract_mirror.mirror.enums.ContractType;
import java.util.Map;
@Slf4j
public class Curve3Pool extends CurveBasePool {
    public Curve3Pool(String address, IChainHelper iChainHelper, int coinsCount, Map<String, String> sigMap) {
        super(address, ContractType.CONTRACT_CURVE_3POOL, iChainHelper, coinsCount, sigMap);
    }
}
