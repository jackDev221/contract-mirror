package org.tron.sunio.contract_mirror.mirror.contracts.factory;

import org.tron.sunio.contract_mirror.mirror.chainHelper.IChainHelper;
import org.tron.sunio.contract_mirror.mirror.contracts.BaseContract;
import org.tron.sunio.contract_mirror.mirror.contracts.IContractFactory;
import org.tron.sunio.contract_mirror.mirror.db.IDbHandler;
import org.tron.sunio.contract_mirror.mirror.enums.ContractType;

import java.util.List;
import java.util.Map;

public class SwapFactoryV2 extends BaseContract implements IContractFactory {

    public SwapFactoryV2(String address, ContractType type, IChainHelper iChainHelper, IDbHandler iDbHandler,
                         Map<String, String> sigMap) {
        super(address, type, iChainHelper, iDbHandler, sigMap);
    }

    @Override
    public BaseContract getBaseContract() {
        return null;
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
    public String getFactoryState() {
        return null;
    }
}
