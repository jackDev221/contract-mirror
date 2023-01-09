package org.tron.sunio.contract_mirror.mirror.contracts;

import cn.hutool.core.util.ObjectUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.tron.sunio.contract_mirror.mirror.chainHelper.IChainHelper;
import org.tron.sunio.contract_mirror.mirror.chainHelper.TronChainHelper;
import org.tron.sunio.contract_mirror.mirror.contracts.factory.ContractFactoryV1;
import org.tron.sunio.contract_mirror.mirror.servers.ContractMirror;

import java.util.HashMap;
import java.util.List;

@Component
@Slf4j
public class ContractFactoryManager {
    @Autowired
    private ContractMirror contractMirror;

    @Autowired
    private TronChainHelper tronChainHelper;
    private HashMap<String, IContractFactory> contractFactoryHashMap = new HashMap<>();

    public boolean initFactoryMap(List<ContractInfo> contractInfoList) {
        log.info("ContractFactoryManager.initFactoryMap: create Factory!");
        if (ObjectUtil.isNull(contractInfoList) || contractInfoList.size() == 0){
            return true;
        }
        for (ContractInfo contractInfo : contractInfoList) {
            switch (contractInfo.getContractType()) {
                case CONTRACT_FACTORY_V1:
                    contractFactoryHashMap.put(contractInfo.getAddress(), new ContractFactoryV1(
                            contractInfo.getAddress(),
                            tronChainHelper
                    ));
                    break;
                default:
                    break;
            }
        }
        log.info("ContractFactoryManager.initFactoryMap: init Factory!");
        for (String addr : this.contractFactoryHashMap.keySet()) {
            IContractFactory iContractFactory = this.contractFactoryHashMap.get(addr);
            BaseContract baseContract = iContractFactory.getBaseContract();
            baseContract.initContract();
            baseContract.setReady(true);
        }
        return true;
    }

    public boolean updateMirrorContracts() {
        log.info("ContractFactoryManager: start updateMirrorContracts");
        for (String addr : this.contractFactoryHashMap.keySet()) {
            IContractFactory iContractFactory = this.contractFactoryHashMap.get(addr);
            BaseContract baseContract = iContractFactory.getBaseContract();
            HashMap<String, BaseContract> contractHashMap = this.contractMirror.getContractHashMap();
            if (!contractHashMap.containsKey(addr)) {
                contractHashMap.put(baseContract.address, baseContract);
            }

            List<BaseContract> baseContractList = iContractFactory.getListContracts();
            for (BaseContract baseContract1 : baseContractList) {
                if (contractHashMap.containsKey(baseContract1.address)) {
                    continue;
                }
                contractHashMap.put(baseContract1.address, baseContract1);
            }
        }
        return true;
    }
}
