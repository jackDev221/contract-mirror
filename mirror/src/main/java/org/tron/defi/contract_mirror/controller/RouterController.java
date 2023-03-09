package org.tron.defi.contract_mirror.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.tron.defi.contract_mirror.dao.RouterPath;
import org.tron.defi.contract_mirror.dto.legacy.RouterResultV2;
import org.tron.defi.contract_mirror.service.RouterService;

import java.math.BigInteger;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/swap/")
public class RouterController {
    @Autowired
    RouterService routerService;

    @GetMapping("/routingInV2")
    public Response routerV2(@RequestParam(name = "fromToken", required = true) String fromToken,
                             @RequestParam(name = "toToken", required = true) String toToken,
                             @RequestParam(name = "fromTokenAddr", required = true) String from,
                             @RequestParam(name = "toTokenAddr", required = true) String to,
                             @RequestParam(name = "inAmount", required = true) String amount,
                             @RequestParam(name = "fromDecimal", required = true) int fromDecimal,
                             @RequestParam(name = "toDecimal", required = true) int toDecimal) {
        Response response = new Response<>();
        BigInteger amountIn = new BigInteger(amount);
        try {
            List<RouterPath> paths = routerService.getPath(from, to, amountIn);
            response.setCode(Response.Code.SUCCESS);
            List<RouterResultV2> resultV2s = paths.stream()
                                                  .map(RouterResultV2::fromRouterPath)
                                                  .collect(Collectors.toList());
            response.setData(resultV2s);
        } catch (RuntimeException e) {
            response.setCode(Response.Code.ERROR);
            response.setMessage(e.getMessage());
        }
        return response;
    }
}
