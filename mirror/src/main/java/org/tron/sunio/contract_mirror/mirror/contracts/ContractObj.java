package org.tron.sunio.contract_mirror.mirror.contracts;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.tron.sunio.contract_mirror.mirror.chainHelper.IChainHelper;
import org.tron.sunio.contract_mirror.mirror.enums.ContractType;

import java.math.BigInteger;
import java.util.Map;

@Data
@Slf4j
public class ContractObj implements IContract{
    protected String address;
    protected ContractType type;
    protected boolean isReady;
    protected boolean isUsing;
    protected boolean isAddExchangeContracts;
    protected boolean isDirty;
    protected Map<String, String> sigMap;
    protected IChainHelper iChainHelper;
    @Getter
    @Setter
    protected IContractsHelper iContractsHelper;

    @Override
    public ContractType getContractType() {
        return this.getType();
    }

    public ContractObj(String address, ContractType type, IChainHelper iChainHelper, IContractsHelper iContractsHelper,
                        final Map<String, String> sigMap) {
        this.type = type;
        this.address = address;
        this.iChainHelper = iChainHelper;
        this.sigMap = sigMap;
        this.isUsing = true;
        this.iContractsHelper = iContractsHelper;
    }

    protected BigInteger getBalance(String address) {
        return iChainHelper.balance(address);
    }

}
