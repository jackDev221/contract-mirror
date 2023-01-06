package org.tron.sunio.contract_mirror.mirror.contracts.factory;

import org.tron.sunio.contract_mirror.mirror.chainHelper.IChainHelper;
import org.tron.sunio.contract_mirror.mirror.contracts.BaseContract;
import org.tron.sunio.contract_mirror.mirror.contracts.IContractFactory;
import org.tron.sunio.contract_mirror.mirror.enums.ContractType;

import java.util.List;

public class ContractFactoryV1 extends BaseContract implements IContractFactory {
    public ContractFactoryV1(String address, IChainHelper iChainHelper) {
        super(address, iChainHelper);
    }

    @Override
    public List<BaseContract> getListContracts() {
        return null;
    }

    @Override
    public List<String> getListContractAddresses() {
        return null;
    }

    @Override
    public ContractType getContract() {
        return ContractType.CONTRACT_FACTORY_V1;
    }
}
