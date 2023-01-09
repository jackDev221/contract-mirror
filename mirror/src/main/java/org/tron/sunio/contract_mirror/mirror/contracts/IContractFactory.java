package org.tron.sunio.contract_mirror.mirror.contracts;

import java.util.List;

public interface IContractFactory {
    BaseContract getBaseContract();

    List<BaseContract> getListContracts();

    List<String> getListContractAddresses();

    String getFactoryState();
}
