package org.tron.defi.contract_mirror.controller;

import com.alibaba.fastjson.JSON;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.tron.defi.contract_mirror.service.ContractService;

@Controller
public class ContractController {
    @Autowired
    ContractService contractService;

    @RequestMapping("/contract/{address}/{method}")
    @ResponseBody
    public String callContract(@PathVariable String address, @PathVariable String method) {
        Response response = new Response();
        try {
            response.setContent(contractService.call(address, method));
            response.setCode(Response.Code.SUCCESS);
        } catch (Exception e) {
            response.setCode(Response.Code.ERROR);
            response.setContent(e.getMessage());
        }
        return JSON.toJSONString(response);
    }

    @RequestMapping("/info/{address}")
    public String contractInfo(@PathVariable String address) {
        Response response = new Response();
        try {
            response.setContent(contractService.info(address));
            response.setCode(Response.Code.SUCCESS);
        } catch (Exception e) {
            response.setCode(Response.Code.ERROR);
            response.setContent(e.getMessage());
        }
        return JSON.toJSONString(response);
    }
}
