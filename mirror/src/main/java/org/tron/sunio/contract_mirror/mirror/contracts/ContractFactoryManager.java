package org.tron.sunio.contract_mirror.mirror.contracts;

import cn.hutool.core.util.ObjectUtil;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.tron.sunio.contract_mirror.event_decode.events.SwapV1Event;
import org.tron.sunio.contract_mirror.event_decode.events.SwapV1FactoryEvent;
import org.tron.sunio.contract_mirror.mirror.chainHelper.IChainHelper;
import org.tron.sunio.contract_mirror.mirror.chainHelper.TronChainHelper;
import org.tron.sunio.contract_mirror.mirror.contracts.factory.ContractFactoryV1;
import org.tron.sunio.contract_mirror.mirror.servers.ContractMirror;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
@Slf4j
public class ContractFactoryManager {
    @Setter
    private ContractMirror contractMirror;
    @Autowired
    private IChainHelper tronChainHelper;
    private HashMap<String, IContractFactory> contractFactoryHashMap = new HashMap<>();
    private Map<String, String> v1FactorySigMap;

    private void initSigMaps() {

        v1FactorySigMap = SwapV1FactoryEvent.getSigMap();
    }

    public boolean initFactoryMap(List<ContractInfo> contractInfoList) {
        log.info("ContractFactoryManager.initFactoryMap: create Factory!");
        initSigMaps();
        if (ObjectUtil.isNull(contractInfoList) || contractInfoList.size() == 0) {
            return true;
        }
        for (ContractInfo contractInfo : contractInfoList) {
            switch (contractInfo.getContractType()) {
                case CONTRACT_FACTORY_V1:
                    contractFactoryHashMap.put(contractInfo.getAddress(), new ContractFactoryV1(
                            contractInfo.getAddress(),
                            tronChainHelper,
                            v1FactorySigMap
                    ));
                    break;
                default:
                    break;
            }
        }
        HashMap<String, BaseContract> contractHashMap = this.contractMirror.getContractHashMap();
        log.info("ContractFactoryManager.initFactoryMap: init Factory!");
        for (String addr : this.contractFactoryHashMap.keySet()) {
            IContractFactory iContractFactory = this.contractFactoryHashMap.get(addr);
            BaseContract baseContract = iContractFactory.getBaseContract();
            contractHashMap.put(baseContract.address, baseContract);
        }
        return true;
    }

    public boolean updateMirrorContracts() {
        log.info("ContractFactoryManager: start updateMirrorContracts");
        HashMap<String, BaseContract> contractHashMap = this.contractMirror.getContractHashMap();
        for (String addr : this.contractFactoryHashMap.keySet()) {
            IContractFactory iContractFactory = this.contractFactoryHashMap.get(addr);
            BaseContract baseContract = iContractFactory.getBaseContract();
            if (!baseContract.isReady() || baseContract.isAddExchangeContracts()) {
                continue;
            }
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
            baseContract.setAddExchangeContracts(true);
            baseContract.updateBaseInfoToCache(baseContract.isUsing, baseContract.isReady, baseContract.isAddExchangeContracts);
        }
        return true;
    }
}
