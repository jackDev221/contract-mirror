package org.tron.sunio.contract_mirror.mirror.controller;

import cn.hutool.core.util.ObjectUtil;
import io.swagger.annotations.ApiParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.tron.sunio.contract_mirror.mirror.contracts.BaseContract;
import org.tron.sunio.contract_mirror.mirror.controller.request.ContractCallParams;
import org.tron.sunio.contract_mirror.mirror.enums.ResponseEnum;
import org.tron.sunio.contract_mirror.mirror.response.RestResultGenerator;
import org.tron.sunio.contract_mirror.mirror.response.ResultResponse;
import org.tron.sunio.contract_mirror.mirror.router.RoutItem;
import org.tron.sunio.contract_mirror.mirror.router.RouterInput;
import org.tron.sunio.contract_mirror.mirror.servers.ContractMirror;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.List;

import static org.tron.sunio.contract_mirror.mirror.consts.ContractMirrorConst.CONTRACT_CONST_METHOD;
import static org.tron.sunio.contract_mirror.mirror.consts.ContractMirrorConst.CONTRACT_ROUTING;


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
        try {
            var res = baseContract.handRequest(method, "");
            return RestResultGenerator.genResult(res);
        } catch (Exception e) {
            return RestResultGenerator.genErrorWithMessage(e.getMessage());
        }
    }

    @PostMapping(value = CONTRACT_CONST_METHOD)
    ResultResponse<Object> queryContractConstMethodWithParams(
            @ApiParam(name = "address", value = "合约地址") @PathVariable("address") String address,
            @ApiParam(name = "method", value = "合约方法") @PathVariable("method") String method,
            @RequestBody ContractCallParams param) {
        BaseContract baseContract = contractMirror.getContract(address);
        if (ObjectUtil.isNull(baseContract)) {
            return RestResultGenerator.genErrorResult(ResponseEnum.SERVER_ERROR);
        }
        try {
            var res = baseContract.handRequest(method, param.getParams());
            return RestResultGenerator.genResult(res);
        } catch (Exception e) {
            return RestResultGenerator.genErrorWithMessage(e.getMessage());
        }
    }

    @GetMapping(value = CONTRACT_ROUTING)
    public ResultResponse<Object> routerV2(@RequestParam(name = "fromToken", required = true) String fromToken,
                                           @RequestParam(name = "toToken", required = true) String toToken,
                                           @RequestParam(name = "fromTokenAddr", required = true) String fromTokenAddr,
                                           @RequestParam(name = "toTokenAddr", required = true) String toTokenAddr,
                                           @RequestParam(name = "inAmount", required = true) String inAmount,
                                           @RequestParam(name = "fromDecimal", required = true) int fromDecimal,
                                           @RequestParam(name = "toDecimal", required = true) int toDecimal,
                                           @RequestParam(name = "useBaseTokens", required = false) boolean isUseBaseToken
    ) {

        try {
            if (!contractMirror.isFirstFinishLoadData()) {
                return RestResultGenerator.genErrorWithMessage("Load contracts not finished");
            }
            String[] prices = contractMirror.getRouterServer().getTokenPrice(fromTokenAddr, toTokenAddr, fromToken, toToken);
            RouterInput routerInput = new RouterInput(fromTokenAddr, toTokenAddr, fromToken, toToken, fromDecimal, toDecimal,
                    new BigInteger(inAmount), new BigDecimal(prices[0]), new BigDecimal(prices[1]), isUseBaseToken);
            List<RoutItem> res = contractMirror.getRouterServer().getRouter(routerInput, contractMirror.getContractHashMap());
            return RestResultGenerator.genResult(res);
        } catch (Exception e) {
            e.printStackTrace();
            return RestResultGenerator.genErrorWithMessage(e.getMessage());
        }
    }
}
