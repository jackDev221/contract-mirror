package org.tron.sunio.contract_mirror.mirror.contracts;

import cn.hutool.core.util.ObjectUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.tron.sunio.contract_mirror.event_decode.events.Curve2PoolEvent;
import org.tron.sunio.contract_mirror.event_decode.events.Curve3PoolEvent;
import org.tron.sunio.contract_mirror.event_decode.events.Curve4PoolEvent;
import org.tron.sunio.contract_mirror.event_decode.events.PSMEvent;
import org.tron.sunio.contract_mirror.event_decode.events.SwapV1FactoryEvent;
import org.tron.sunio.contract_mirror.event_decode.events.SwapV2FactoryEvent;
import org.tron.sunio.contract_mirror.mirror.chainHelper.IChainHelper;
import org.tron.sunio.contract_mirror.mirror.contracts.factory.BaseFactory;
import org.tron.sunio.contract_mirror.mirror.contracts.factory.SwapFactoryV1;
import org.tron.sunio.contract_mirror.mirror.contracts.factory.SwapFactoryV2;
import org.tron.sunio.contract_mirror.mirror.contracts.impl.Assemble3Pool;
import org.tron.sunio.contract_mirror.mirror.contracts.impl.Assemble4Pool;
import org.tron.sunio.contract_mirror.mirror.contracts.impl.BaseStableSwapPool;
import org.tron.sunio.contract_mirror.mirror.contracts.impl.Curve2Pool;
import org.tron.sunio.contract_mirror.mirror.contracts.impl.Curve3Pool;
import org.tron.sunio.contract_mirror.mirror.contracts.impl.PSM;
import org.tron.sunio.contract_mirror.mirror.dao.PSMTotalData;
import org.tron.sunio.contract_mirror.mirror.pool.CMPool;


import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
@Slf4j
public class ContractFactoryManager {
    @Autowired
    private IChainHelper tronChainHelper;

    @Autowired
    private CMPool cmPool;

    @Autowired
    private PSMTotalData psmTotalData;
    private List<String> psmContracts = new ArrayList<>();

    private HashMap<String, IContractFactory> contractFactoryHashMap = new HashMap<>();
    private Map<String, String> v1FactorySigMap;
    private Map<String, String> v2FactorySigMap;
    private Map<String, String> curve2PoolSigMap;
    private Map<String, String> curve3PoolSigMap;
    private Map<String, String> curve4PoolSigMap;
    private Map<String, String> psmSigMap;


    private void initSigMaps() {
        v1FactorySigMap = SwapV1FactoryEvent.getSigMap();
        v2FactorySigMap = SwapV2FactoryEvent.getSigMap();
        curve2PoolSigMap = Curve2PoolEvent.getSigMap();
        curve3PoolSigMap = Curve3PoolEvent.getSigMap();
        curve4PoolSigMap = Curve4PoolEvent.getSigMap();
        psmSigMap = PSMEvent.getSigMap();
    }

    public boolean initFactoryMap(List<ContractInfo> contractInfoList, IContractsCollectHelper iContractsCollectHelper, IContractsHelper iContractsHelper) {
        log.info("ContractFactoryManager.initFactoryMap: create Factory!");
        initSigMaps();
        if (ObjectUtil.isNull(contractInfoList) || contractInfoList.size() == 0) {
            return true;
        }
        for (ContractInfo contractInfo : contractInfoList) {
            switch (contractInfo.getType()) {
                case SWAP_FACTORY_V1:
                    contractFactoryHashMap.put(contractInfo.getAddress(), new SwapFactoryV1(
                            contractInfo.getAddress(),
                            tronChainHelper,
                            iContractsHelper,
                            v1FactorySigMap
                    ));
                    break;
                case SWAP_FACTORY_V2:
                    contractFactoryHashMap.put(contractInfo.getAddress(), new SwapFactoryV2(
                            contractInfo.getAddress(),
                            tronChainHelper,
                            iContractsHelper,
                            v2FactorySigMap

                    ));
                    break;
                case CONTRACT_CURVE_2POOL:
                    iContractsCollectHelper.addContract(new Curve2Pool(
                            contractInfo.getAddress(),
                            tronChainHelper,
                            iContractsHelper,
                            2,
                            1,
                            curve2PoolSigMap
                    ));
                    break;
                case CONTRACT_CURVE_3POOL:
                    iContractsCollectHelper.addContract(new Curve3Pool(
                            contractInfo.getAddress(),
                            tronChainHelper,
                            iContractsHelper,
                            3,
                            2,
                            curve3PoolSigMap
                    ));
                    break;

                case CONTRACT_ASSEMBLE_3POOL:
                    iContractsCollectHelper.addContract(new Assemble3Pool(
                            contractInfo.getAddress(),
                            tronChainHelper,
                            iContractsHelper,
                            new HashMap<>()
                    ));
                    break;

                case CONTRACT_ASSEMBLE_4POOL:
                    iContractsCollectHelper.addContract(new Assemble4Pool(
                            contractInfo.getAddress(),
                            tronChainHelper,
                            iContractsHelper,
                            new HashMap<>()
                    ));
                    break;

                case CONTRACT_PSM_USDT:
                case CONTRACT_PSM_USDC:
                case CONTRACT_PSM_USDJ:
                case CONTRACT_PSM_TUSD:
                    PSM psm = PSM.genInstance(contractInfo, tronChainHelper, iContractsHelper, psmTotalData, psmSigMap);
                    if (ObjectUtil.isNotNull(psm)) {
                        psmContracts.add(contractInfo.getAddress());
                        iContractsCollectHelper.addContract(psm);
                    }else {
                        log.error("Fail to create instance for address: {}, type: {}, extra: {}", contractInfo.getAddress(),
                                contractInfo.getType(), contractInfo.getExtra());
                    }
                    break;
                case STABLE_SWAP_POOL:
                    BaseStableSwapPool instance = BaseStableSwapPool.genInstance(contractInfo, tronChainHelper,
                            iContractsHelper, curve2PoolSigMap);
                    if (ObjectUtil.isNotNull(instance)) {
                        iContractsCollectHelper.addContract(instance);
                    } else {
                        log.error("Fail to create instance for address: {}, type: {}, extra: {}", contractInfo.getAddress(),
                                contractInfo.getType(), contractInfo.getExtra());
                    }
                    break;
                default:
                    break;
            }
        }
        log.info("ContractFactoryManager.initFactoryMap: init Factory!");
        for (String addr : this.contractFactoryHashMap.keySet()) {
            IContractFactory iContractFactory = this.contractFactoryHashMap.get(addr);
            iContractsCollectHelper.addContract(iContractFactory.getBaseContract());
        }
        return true;
    }

    private void updatePsmTotalData(IContractsCollectHelper iContractsCollectHelper) {
        if (psmTotalData.isFinishInit() || psmContracts.size() == 0) {
            return;
        }
        for (String contract : psmContracts) {
            BaseContract baseContract = iContractsCollectHelper.getContract(contract);
            if (ObjectUtil.isNull(baseContract) || !baseContract.isReady) {
                return;
            }
        }
        PSM psm = (PSM) iContractsCollectHelper.getContract(psmContracts.get(0));
        BigInteger[] totalInfos = psm.getTotalInfos();
        for (String contract : psmContracts) {
            PSM item = (PSM) iContractsCollectHelper.getContract(contract);
            item.updateTotalInfos(totalInfos);
        }
        psmTotalData.setTotalMaxSwapUSDD(totalInfos[0]);
        psmTotalData.setTotalSwappedUSDD(totalInfos[1]);
        psmTotalData.setFinishInit(true);
    }

    public void resetPsmTotalDataState() {
        psmTotalData.setFinishInit(false);
    }


    public boolean updateMirrorContracts(IContractsCollectHelper iContractsCollectHelper) {
        updatePsmTotalData(iContractsCollectHelper);
//        log.info("ContractFactoryManager: start updateMirrorContracts");
        for (String addr : this.contractFactoryHashMap.keySet()) {
            IContractFactory iContractFactory = this.contractFactoryHashMap.get(addr);
            BaseFactory baseContract = iContractFactory.getBaseContract();
            if (!baseContract.isReady() || baseContract.isAddExchangeContracts()) {
                continue;
            }
            if (!iContractsCollectHelper.containsContract(addr)) {
                iContractsCollectHelper.addContract(baseContract);
            }

            List<BaseContract> baseContractList = iContractFactory.getListContracts(cmPool);
            for (BaseContract baseContract1 : baseContractList) {
                if (iContractsCollectHelper.containsContract(baseContract1.address)) {
                    continue;
                }
                iContractsCollectHelper.addContract(baseContract1);
            }
            baseContract.resetLoadSubContractState();
            baseContract.updateBaseInfo(baseContract.isUsing, baseContract.isReady, baseContract.isAddExchangeContracts);
        }
        return true;
    }
}
