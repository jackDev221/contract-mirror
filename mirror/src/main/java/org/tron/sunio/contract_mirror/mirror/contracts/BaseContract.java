package org.tron.sunio.contract_mirror.mirror.contracts;

import lombok.Data;
import org.tron.sunio.contract_mirror.mirror.chainHelper.IChainHelper;
import org.tron.sunio.contract_mirror.mirror.enums.ContractType;

@Data
public class BaseContract implements IContract {
    private String address;
    private boolean isReady;
    private boolean isUsing;
    private IChainHelper iChainHelper;

    public BaseContract(String address, IChainHelper iChainHelper){
        this.address = address;
        this.iChainHelper = iChainHelper;
    }

    @Override
    public boolean initContract() {
        return false;
    }

    @Override
    public ContractType getContract() {
        return null;
    }
}
