package org.tron.defi.contract_mirror.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.tron.defi.contract_mirror.dto.Response;
import org.tron.defi.contract_mirror.service.ContractService;

@RestController
public class ContractController {
    @Autowired
    ContractService contractService;

    @RequestMapping("/contract/{address}/{method}")
    public Response callContract(@PathVariable String address, @PathVariable String method) {
        Response response = new Response();
        try {
            response.setData(contractService.call(address, method));
            response.setCode(Response.Code.SUCCESS);
        } catch (Exception e) {
            response.setCode(Response.Code.ERROR);
            response.setMessage(e.getMessage());
        }
        return response;
    }

    @GetMapping("/info/{address}")
    public Response contractInfo(@PathVariable String address) {
        Response response = new Response();
        try {
            response.setCode(Response.Code.SUCCESS);
            response.setData(contractService.info(address));
        } catch (Exception e) {
            response.setCode(Response.Code.ERROR);
            response.setMessage(e.getMessage());
        }
        return response;
    }
}
