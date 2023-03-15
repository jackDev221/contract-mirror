package org.tron.defi.contract_mirror.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.tron.defi.contract_mirror.core.token.IToken;
import org.tron.defi.contract_mirror.dao.RouterPath;
import org.tron.defi.contract_mirror.dto.Response;
import org.tron.defi.contract_mirror.dto.legacy.RouterResultV2;
import org.tron.defi.contract_mirror.service.PriceService;
import org.tron.defi.contract_mirror.service.RouterService;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/swap/")
public class RouterController {
    @Autowired
    RouterService routerService;
    @Autowired
    PriceService priceService;

    @GetMapping("/routingInV2")
    public Response routerV2(@RequestParam(name = "fromToken", required = true) String fromToken,
                             @RequestParam(name = "toToken", required = true) String toToken,
                             @RequestParam(name = "fromTokenAddr", required = true) String from,
                             @RequestParam(name = "toTokenAddr", required = true) String to,
                             @RequestParam(name = "inAmount", required = true) String amount,
                             @RequestParam(name = "fromDecimal", required = true) int fromDecimal,
                             @RequestParam(name = "toDecimal", required = true) int toDecimal) {
        log.trace("Request fromToken={} toToken={} fromTokenAddr={} toTokenAddr={} inAmount={} " +
                  "fromDecimal={} toDecimal={}",
                  fromToken,
                  toToken,
                  from,
                  to,
                  amount,
                  fromDecimal,
                  toDecimal);
        Response response = new Response<>();
        BigInteger amountIn = new BigInteger(amount);
        try {
            long time0 = System.currentTimeMillis();
            BigDecimal inUsdPrice = priceService.getPrice(from);
            BigDecimal outUsdPrice = priceService.getPrice(to);
            log.debug("inUsdPrice={} outUsdPrice={}", inUsdPrice, outUsdPrice);
            long time1 = System.currentTimeMillis();
            List<RouterPath> paths = routerService.getPath(from, to, amountIn);
            long time2 = System.currentTimeMillis();
            List<RouterResultV2> resultV2s = new ArrayList<>(paths.size());
            BigDecimal inUsd = null;
            if (!paths.isEmpty()) {
                IToken inToken = (IToken) paths.get(0).getFrom().getToken();
                inUsd = new BigDecimal(amount).divide(BigDecimal.valueOf(10)
                                                                .pow(inToken.getDecimals()),
                                                      inToken.getDecimals(),
                                                      RoundingMode.HALF_UP).multiply(inUsdPrice);
            }
            for (int i = 0; i < paths.size(); i++) {
                RouterPath path = paths.get(i);
                RouterResultV2 resultV2 = RouterResultV2.fromRouterPath(path);
                resultV2.setInUsd(inUsd.toString());
                resultV2.setOutUsd(new BigDecimal(resultV2.getAmount()).multiply(outUsdPrice)
                                                                       .toString());
                resultV2s.add(resultV2);
            }
            response.setCode(Response.Code.SUCCESS);
            response.setData(resultV2s);
            long time3 = System.currentTimeMillis();
            log.trace("{}, timePrice {}ms, timePath {}ms, timeResponse {}ms",
                      response,
                      time1 - time0,
                      time2 - time1,
                      time3 - time2);
        } catch (RuntimeException e) {
            response.setCode(Response.Code.ERROR);
            response.setMessage(e.getMessage());
        }
        return response;
    }
}
