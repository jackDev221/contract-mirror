package org.tron.sunio.contract_mirror.mirror.db;

import org.tron.sunio.contract_mirror.mirror.dao.SwapFactoryV1Data;
import org.tron.sunio.contract_mirror.mirror.dao.SwapV1Data;

public interface IDbHandler {
    SwapV1Data queryContractV1Data(String address);

    void updateContractV1Data(SwapV1Data swapV1Data);

    SwapFactoryV1Data queryContractFactoryV1Data(String address);

    void updateContractFactoryV1Data(SwapFactoryV1Data factoryV1Data);

}
