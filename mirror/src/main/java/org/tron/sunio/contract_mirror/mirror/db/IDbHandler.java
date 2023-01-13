package org.tron.sunio.contract_mirror.mirror.db;

import org.tron.sunio.contract_mirror.mirror.dao.ContractFactoryV1Data;
import org.tron.sunio.contract_mirror.mirror.dao.ContractV1Data;

public interface IDbHandler {
    ContractV1Data queryContractV1Data(String address);

    void updateContractV1Data(ContractV1Data contractV1Data);

    ContractFactoryV1Data queryContractFactoryV1Data(String address);

    void updateContractFactoryV1Data(ContractFactoryV1Data factoryV1Data);

}
