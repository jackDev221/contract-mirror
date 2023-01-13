package org.tron.sunio.contract_mirror.mirror.controller;

import cn.hutool.core.util.ObjectUtil;
import io.swagger.annotations.ApiParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import org.tron.sunio.contract_mirror.mirror.db.IDbHandler;
import org.tron.sunio.contract_mirror.mirror.dao.ContractFactoryV1Data;
import org.tron.sunio.contract_mirror.mirror.dao.ContractV1Data;
import org.tron.sunio.contract_mirror.mirror.enums.ResponseEnum;
import org.tron.sunio.contract_mirror.mirror.response.RestResultGenerator;
import org.tron.sunio.contract_mirror.mirror.response.ResultResponse;

@RestController
public class ContractStatusController {
    @Autowired
    private IDbHandler iDbHandler;

    @GetMapping(value = "/swapv1/exchange/{address}/status")
    ResultResponse<ContractV1Data> queryContractV1ExchangeStatus(
            @ApiParam(name = "address", value = "合约地址") @PathVariable("address") String address) {
        ContractV1Data v1Data = iDbHandler.queryContractV1Data(address);
        if (ObjectUtil.isNull(v1Data)) {
            return RestResultGenerator.genErrorResult(ResponseEnum.SERVER_ERROR);
        }
        return RestResultGenerator.genResult(v1Data);
    }

    @GetMapping(value = "/swapv1/factory/{address}/status")
    ResultResponse<ContractFactoryV1Data> queryContractV1FactoryStatus(
            @ApiParam(name = "address", value = "合约地址") @PathVariable("address") String address) {
        ContractFactoryV1Data v1Data = iDbHandler.queryContractFactoryV1Data(address);
        if (ObjectUtil.isNull(v1Data)) {
            return RestResultGenerator.genErrorResult(ResponseEnum.SERVER_ERROR);
        }
        return RestResultGenerator.genResult(v1Data);
    }
}
