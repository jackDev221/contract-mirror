package org.tron.sunio.contract_mirror.mirror.db;

import org.tron.sunio.contract_mirror.mirror.dao.Curve2PoolData;
import org.tron.sunio.contract_mirror.mirror.dao.Curve3PoolData;
import org.tron.sunio.contract_mirror.mirror.dao.SwapFactoryV1Data;
import org.tron.sunio.contract_mirror.mirror.dao.SwapFactoryV2Data;
import org.tron.sunio.contract_mirror.mirror.dao.SwapV1Data;
import org.tron.sunio.contract_mirror.mirror.dao.SwapV2PairData;

public interface IDbHandler {
    SwapV1Data querySwapV1Data(String address);

    void updateSwapV1Data(SwapV1Data swapV1Data);

    SwapFactoryV1Data querySwapFactoryV1Data(String address);

    void updateSwapFactoryV1Data(SwapFactoryV1Data factoryV1Data);

    SwapV2PairData querySwapV2PairData(String address);

    void updateSwapV2PairData(SwapV2PairData swapV1Data);

    SwapFactoryV2Data querySwapFactoryV2Data(String address);

    void updateSwapFactoryV2Data(SwapFactoryV2Data factoryV1Data);

    Curve2PoolData queryCurve2PoolData(String address);

    void updateCurve2PoolData(Curve2PoolData factoryV1Data);

    Curve3PoolData queryCurve3PoolData(String address);

    void updateCurve3PoolData(Curve3PoolData factoryV1Data);
}
