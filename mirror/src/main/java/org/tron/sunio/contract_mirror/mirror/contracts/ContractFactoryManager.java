package org.tron.sunio.contract_mirror.mirror.contracts;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.tron.sunio.contract_mirror.mirror.servers.ContractMirror;

import java.util.HashMap;
import java.util.List;

@Component
public class ContractFactoryManager {
    @Autowired
    private ContractMirror contractMirror;
    private HashMap<String, IContractFactory> contractFactoryHashMap = new HashMap<>();

    public  boolean initFactoryMap(List<ContractInfo> contractInfoList){
        return true;
    }
}
