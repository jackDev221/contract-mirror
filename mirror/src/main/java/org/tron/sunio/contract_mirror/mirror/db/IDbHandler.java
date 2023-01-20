package org.tron.sunio.contract_mirror.mirror.db;

import org.tron.sunio.contract_mirror.mirror.dao.Curve2PoolData;
import org.tron.sunio.contract_mirror.mirror.dao.Curve3PoolData;
import org.tron.sunio.contract_mirror.mirror.dao.Curve4PoolData;
import org.tron.sunio.contract_mirror.mirror.dao.PSMData;
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

    void updateSwapFactoryV2Data(SwapFactoryV2Data factory21Data);

    Curve2PoolData queryCurve2PoolData(String address);

    void updateCurve2PoolData(Curve2PoolData curve2PoolData);

    Curve3PoolData queryCurve3PoolData(String address);

    void updateCurve3PoolData(Curve3PoolData curve3PoolData);

    Curve4PoolData queryCurve4PoolData(String address);

    void updateCurve4PoolData(Curve4PoolData curve4PoolData);


    PSMData queryPSMData(String address);

    void updatePSMData(PSMData psmData);
}
