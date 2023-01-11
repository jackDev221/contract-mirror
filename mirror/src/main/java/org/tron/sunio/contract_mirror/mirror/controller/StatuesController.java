package org.tron.sunio.contract_mirror.mirror.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class StatuesController {
    @GetMapping(path = "/mstate")
    public String getMStatus() {
        return "Every thing is OK";
    }
}
