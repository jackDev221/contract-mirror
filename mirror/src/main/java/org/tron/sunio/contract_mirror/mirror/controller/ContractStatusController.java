package org.tron.sunio.contract_mirror.mirror.controller;

import cn.hutool.core.util.ObjectUtil;
import io.swagger.annotations.ApiParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import org.tron.sunio.contract_mirror.mirror.contracts.BaseContract;
import org.tron.sunio.contract_mirror.mirror.enums.ResponseEnum;
import org.tron.sunio.contract_mirror.mirror.response.RestResultGenerator;
import org.tron.sunio.contract_mirror.mirror.response.ResultResponse;
import org.tron.sunio.contract_mirror.mirror.servers.ContractMirror;

import static org.tron.sunio.contract_mirror.mirror.consts.ContractMirrorConst.CONTRACT_CONST_METHOD;


@RestController
public class ContractStatusController {
    @Autowired
    private ContractMirror contractMirror;

    @GetMapping(value = CONTRACT_CONST_METHOD)
    ResultResponse<Object> queryContractConstMethod(
            @ApiParam(name = "address", value = "合约地址") @PathVariable("address") String address,
            @ApiParam(name = "method", value = "合约方法") @PathVariable("method") String method) {
        BaseContract baseContract = contractMirror.getContract(address);
        if (ObjectUtil.isNull(baseContract)) {
            return RestResultGenerator.genErrorResult(ResponseEnum.SERVER_ERROR);
        }
        return RestResultGenerator.genResult(baseContract.handRequest(method));

    }
}
