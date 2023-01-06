package org.tron.sunio.contract_mirror.mirror.contracts.impl;

import org.tron.sunio.contract_mirror.mirror.chainHelper.IChainHelper;
import org.tron.sunio.contract_mirror.mirror.contracts.BaseContract;
import org.tron.sunio.contract_mirror.mirror.enums.ContractType;


public class ContractV1 extends BaseContract {

    public ContractV1(String address, IChainHelper iChainHelper) {
        super(address, iChainHelper);
    }

    @Override
    public boolean initContract() {
        return super.initContract();
    }

    @Override
    public ContractType getContract() {
        return ContractType.CONTRACT_V1;
    }
}
