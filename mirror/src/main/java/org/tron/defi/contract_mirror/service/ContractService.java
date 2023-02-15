package org.tron.defi.contract_mirror.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.tron.defi.contract_mirror.core.Contract;
import org.tron.defi.contract_mirror.core.ContractManager;

import javax.annotation.PostConstruct;

@Service
public class ContractService {
    @Autowired
    ContractManager contractManager;
    @Autowired
    EventService eventService;

    @PostConstruct
    public void init() {
        contractManager.init();
        eventService.listen();
    }

    public String call(String address, String method) {
        Contract contract = contractManager.getContract(address);
        if (null == contract) {
            throw new IllegalArgumentException("CONTRACT NOT EXISTS");
        }
        return contract.run(method);
    }

    public String info(String address) {
        Contract contract = contractManager.getContract(address);
        if (null == contract) {
            throw new IllegalArgumentException("CONTRACT NOT EXISTS");
        }
        return contract.info();
    }
}
