package org.tron.sunio.contract_mirror.mirror.contracts;

import java.util.List;

public interface IContractFactory {
    List<BaseContract> getListContracts();
    List<String> getListContractAddresses();
}
