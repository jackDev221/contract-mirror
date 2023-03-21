package org.tron.sunio.contract_mirror.mirror.contracts;

import org.tron.sunio.contract_mirror.mirror.enums.ContractType;

public interface IContract {
    boolean isReady();

    boolean isUsing();

    boolean isAddExchangeContracts();

    ContractType getType();

    String getAddress();
}
