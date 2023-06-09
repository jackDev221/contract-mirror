package org.tron.sunio.contract_mirror.mirror.controller;

import cn.hutool.core.util.ObjectUtil;
import io.swagger.annotations.ApiParam;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.tron.sunio.contract_mirror.mirror.contracts.BaseContract;
import org.tron.sunio.contract_mirror.mirror.controller.request.ContractCallParams;
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
import static org.tron.sunio.contract_mirror.mirror.consts.ContractMirrorConst.CONTRACT_VERSION;
import static org.tron.sunio.contract_mirror.mirror.consts.ContractMirrorConst.METHOD_VERSION;


@RestController
@Slf4j
@RequestMapping("/swap/")
public class ContractStatusController {
    @Autowired
    private ContractMirror contractMirror;

    @GetMapping(value = CONTRACT_CONST_METHOD)
    ResultResponse<Object> queryContractConstMethod(
            @ApiParam(name = "address", value = "合约地址") @PathVariable("address") String address,
            @ApiParam(name = "method", value = "合约方法") @PathVariable("method") String method) {

        try {
            BaseContract baseContract = contractMirror.getContract(address);
            if (ObjectUtil.isNull(baseContract)) {
                return RestResultGenerator.genErrorWithMessage(String.format("Not find contract:%s", address));
            }
            var res = baseContract.handRequest(method, "");
            return RestResultGenerator.genResult(res);
        } catch (Exception e) {
            log.error("Fail to response request: address:{}, method:{}, error:{}", address, method, e.toString());
            return RestResultGenerator.genErrorWithMessage(String.format("Not find contract:%s method:%s", address, method));
        }
    }

    @PostMapping(value = CONTRACT_CONST_METHOD)
    ResultResponse<Object> queryContractConstMethodWithParams(
            @ApiParam(name = "address", value = "合约地址") @PathVariable("address") String address,
            @ApiParam(name = "method", value = "合约方法") @PathVariable("method") String method,
            @RequestBody ContractCallParams param) {
        try {
            BaseContract baseContract = contractMirror.getContract(address);
            if (ObjectUtil.isNull(baseContract)) {
                return RestResultGenerator.genErrorWithMessage(String.format("Not find contract:%s", address));
            }
            var res = baseContract.handRequest(method, param.getParams());
            return RestResultGenerator.genResult(res);
        } catch (Exception e) {
            log.error("Fail to response request: address:{}, method:{}, error:{}", address, method, e.toString());
            return RestResultGenerator.genErrorWithMessage(String.format("Not find contract:%s method:%s", address, method));
        }
    }

    @GetMapping(value = CONTRACT_ROUTING)
    public ResultResponse<Object> routerV2(@RequestParam(name = "fromToken") String fromToken,
                                           @RequestParam(name = "toToken") String toToken,
                                           @RequestParam(name = "fromTokenAddr") String fromTokenAddr,
                                           @RequestParam(name = "toTokenAddr") String toTokenAddr,
                                           @RequestParam(name = "inAmount") String inAmount,
                                           @RequestParam(name = "fromDecimal") int fromDecimal,
                                           @RequestParam(name = "toDecimal") int toDecimal,
                                           @RequestParam(name = "useBaseTokens", required = false, defaultValue = "true") boolean isUseBaseToken
    ) {

        try {
            if (!contractMirror.isFirstFinishLoadData()) {
                log.info("Load contracts not finished");
                return RestResultGenerator.genErrorWithMessage("Load contracts not finished");
            }
            String[] prices = contractMirror.getRouterServer().getTokenPrice(fromTokenAddr, toTokenAddr, fromToken, toToken);
            RouterInput routerInput = new RouterInput(fromTokenAddr, toTokenAddr, fromToken, toToken, fromDecimal, toDecimal,
                    new BigInteger(inAmount), new BigDecimal(prices[0]), new BigDecimal(prices[1]), isUseBaseToken);
            List<RoutItem> res = contractMirror.getRouterServer().getRouter(routerInput, contractMirror.getContractHashMap());
            return RestResultGenerator.genResult(res);
        } catch (Exception e) {
            e.printStackTrace();
            log.error("Fail to response router request, error:{}", e.toString());
            return RestResultGenerator.genErrorWithMessage(e.getMessage());
        }
    }

    @GetMapping(value = CONTRACT_VERSION)
    public ResultResponse<Object> queryContractVersion(
            @ApiParam(name = "address", value = "合约地址") @PathVariable("address") String address) {
        try {
            BaseContract baseContract = contractMirror.getContract(address);
            if (ObjectUtil.isNull(baseContract)) {
                return RestResultGenerator.genErrorWithMessage(String.format("Not find contract:%s", address));
            }
            var res = baseContract.handRequest(METHOD_VERSION, "");
            return RestResultGenerator.genResult(res);
        } catch (Exception e) {
            log.error("Fail to response request: contract:{} version, error:{}", address, e.toString());
            return RestResultGenerator.genErrorWithMessage(String.format("Not find contract:%s", address));
        }
    }
}
