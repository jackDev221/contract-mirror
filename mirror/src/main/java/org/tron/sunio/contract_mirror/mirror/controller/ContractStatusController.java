package org.tron.sunio.contract_mirror.mirror.controller;

import cn.hutool.core.util.ObjectUtil;
import io.swagger.annotations.ApiParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import org.tron.sunio.contract_mirror.mirror.dao.Curve2PoolData;
import org.tron.sunio.contract_mirror.mirror.dao.Curve3PoolData;
import org.tron.sunio.contract_mirror.mirror.dao.PSMData;
import org.tron.sunio.contract_mirror.mirror.dao.SwapFactoryV2Data;
import org.tron.sunio.contract_mirror.mirror.dao.SwapV2PairData;
import org.tron.sunio.contract_mirror.mirror.db.IDbHandler;
import org.tron.sunio.contract_mirror.mirror.dao.SwapFactoryV1Data;
import org.tron.sunio.contract_mirror.mirror.dao.SwapV1Data;
import org.tron.sunio.contract_mirror.mirror.enums.ResponseEnum;
import org.tron.sunio.contract_mirror.mirror.response.RestResultGenerator;
import org.tron.sunio.contract_mirror.mirror.response.ResultResponse;

import static org.tron.sunio.contract_mirror.mirror.consts.ContractMirrorConst.CURVE_2POOL_STATUS;
import static org.tron.sunio.contract_mirror.mirror.consts.ContractMirrorConst.CURVE_3POOL_STATUS;
import static org.tron.sunio.contract_mirror.mirror.consts.ContractMirrorConst.PSM_STATUS;
import static org.tron.sunio.contract_mirror.mirror.consts.ContractMirrorConst.SWAP_V1_EX_STATUS;
import static org.tron.sunio.contract_mirror.mirror.consts.ContractMirrorConst.SWAP_V1_FAC_STATUS;
import static org.tron.sunio.contract_mirror.mirror.consts.ContractMirrorConst.SWAP_V2_FAC_STATUS;
import static org.tron.sunio.contract_mirror.mirror.consts.ContractMirrorConst.SWAP_V2_PAIR_STATUS;

@RestController
public class ContractStatusController {
    @Autowired
    private IDbHandler iDbHandler;

    @GetMapping(value = SWAP_V1_EX_STATUS)
    ResultResponse<SwapV1Data> queryContractV1ExchangeStatus(
            @ApiParam(name = "address", value = "合约地址") @PathVariable("address") String address) {
        SwapV1Data v1Data = iDbHandler.querySwapV1Data(address);
        if (ObjectUtil.isNull(v1Data)) {
            return RestResultGenerator.genErrorResult(ResponseEnum.SERVER_ERROR);
        }
        return RestResultGenerator.genResult(v1Data);
    }

    @GetMapping(value = SWAP_V1_FAC_STATUS)
    ResultResponse<SwapFactoryV1Data> queryContractV1FactoryStatus(
            @ApiParam(name = "address", value = "合约地址") @PathVariable("address") String address) {
        SwapFactoryV1Data v1Data = iDbHandler.querySwapFactoryV1Data(address);
        if (ObjectUtil.isNull(v1Data)) {
            return RestResultGenerator.genErrorResult(ResponseEnum.SERVER_ERROR);
        }
        return RestResultGenerator.genResult(v1Data);
    }

    @GetMapping(value = SWAP_V2_PAIR_STATUS)
    ResultResponse<SwapV2PairData> queryContractV2PairStatus(
            @ApiParam(name = "address", value = "合约地址") @PathVariable("address") String address) {
        SwapV2PairData v2PairData = iDbHandler.querySwapV2PairData(address);
        if (ObjectUtil.isNull(v2PairData)) {
            return RestResultGenerator.genErrorResult(ResponseEnum.SERVER_ERROR);
        }
        return RestResultGenerator.genResult(v2PairData);
    }

    @GetMapping(value = SWAP_V2_FAC_STATUS)
    ResultResponse<SwapFactoryV2Data> queryContractV2FactoryStatus(
            @ApiParam(name = "address", value = "合约地址") @PathVariable("address") String address) {
        SwapFactoryV2Data v2Data = iDbHandler.querySwapFactoryV2Data(address);
        if (ObjectUtil.isNull(v2Data)) {
            return RestResultGenerator.genErrorResult(ResponseEnum.SERVER_ERROR);
        }
        return RestResultGenerator.genResult(v2Data);
    }

    @GetMapping(value = CURVE_2POOL_STATUS)
    ResultResponse<Curve2PoolData> queryContractCurve2PoolStatus(
            @ApiParam(name = "address", value = "合约地址") @PathVariable("address") String address) {
        Curve2PoolData curve2PoolData = iDbHandler.queryCurve2PoolData(address);
        if (ObjectUtil.isNull(curve2PoolData)) {
            return RestResultGenerator.genErrorResult(ResponseEnum.SERVER_ERROR);
        }
        return RestResultGenerator.genResult(curve2PoolData);
    }

    @GetMapping(value = CURVE_3POOL_STATUS)
    ResultResponse<Curve3PoolData> queryContractCurve3PoolStatus(
            @ApiParam(name = "address", value = "合约地址") @PathVariable("address") String address) {
        Curve3PoolData curve3PoolData = iDbHandler.queryCurve3PoolData(address);
        if (ObjectUtil.isNull(curve3PoolData)) {
            return RestResultGenerator.genErrorResult(ResponseEnum.SERVER_ERROR);
        }
        return RestResultGenerator.genResult(curve3PoolData);
    }

    @GetMapping(value = PSM_STATUS)
    ResultResponse<PSMData> queryContractPSMStatus(
            @ApiParam(name = "address", value = "合约地址") @PathVariable("address") String address) {
        PSMData psmData = iDbHandler.queryPSMData(address);
        if (ObjectUtil.isNull(psmData)) {
            return RestResultGenerator.genErrorResult(ResponseEnum.SERVER_ERROR);
        }
        return RestResultGenerator.genResult(psmData);
    }
}
