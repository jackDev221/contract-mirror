package org.tron.sunio.contract_mirror.mirror.contracts;

import org.tron.sunio.contract_mirror.mirror.enums.ContractType;

public interface IContract {
    boolean isReady();

    boolean isUsing();

    boolean initContract();

    ContractType getContract();

    String getAddress();
}
