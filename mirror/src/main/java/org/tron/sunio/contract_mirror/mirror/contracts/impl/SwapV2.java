package org.tron.sunio.contract_mirror.mirror.contracts.impl;

import org.tron.sunio.contract_mirror.mirror.chainHelper.IChainHelper;
import org.tron.sunio.contract_mirror.mirror.contracts.BaseContract;
import org.tron.sunio.contract_mirror.mirror.db.IDbHandler;
import org.tron.sunio.contract_mirror.mirror.enums.ContractType;

import java.util.Map;

public class SwapV2 extends BaseContract {

    public SwapV2(String address, ContractType type, IChainHelper iChainHelper, IDbHandler iDbHandler,
                  Map<String, String> sigMap) {
        super(address, type, iChainHelper, iDbHandler, sigMap);
    }

}
