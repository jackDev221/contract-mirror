package org.tron.sunio.contract_mirror.mirror.contracts;

import cn.hutool.core.lang.Pair;
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
import org.tron.sunio.contract_mirror.mirror.contracts.impl.NewCurvePool;
import org.tron.sunio.contract_mirror.mirror.contracts.impl.OldCurvePool;
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
    private final List<String> psmContracts = new ArrayList<>();

    private final HashMap<String, IContractFactory> contractFactoryHashMap = new HashMap<>();
    private Map<String, String> v1FactorySigMap;
    private Map<String, String> v2FactorySigMap;
    private Map<String, String> curve2PoolSigMap;
    private Map<String, String> curve3PoolSigMap;
    private Map<String, String> psmSigMap;


    private void initSigMaps() {
        v1FactorySigMap = SwapV1FactoryEvent.getSigMap();
        v2FactorySigMap = SwapV2FactoryEvent.getSigMap();
        curve2PoolSigMap = Curve2PoolEvent.getSigMap();
        curve3PoolSigMap = Curve3PoolEvent.getSigMap();
        psmSigMap = PSMEvent.getSigMap();
    }

    public boolean initFactoryMap(List<ContractInfo> contractInfoList, IContractsHelper iContractsHelper) {
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
                case CONTRACT_CURVE_3POOL:
                    OldCurvePool curveBase = OldCurvePool.genInstance(
                            contractInfo, tronChainHelper, iContractsHelper, null
                    );
                    if (ObjectUtil.isNotNull(curveBase)) {
                        if (curveBase.getCoinsCount() == 2) {
                            curveBase.setSigMap(curve2PoolSigMap);
                        } else if (curveBase.getCoinsCount() == 3) {
                            curveBase.setSigMap(curve3PoolSigMap);
                        }
                        iContractsHelper.addContract(curveBase);
                    } else {
                        log.error("Fail to create instance for address: {}, type: {}, extra: {}", contractInfo.getAddress(),
                                contractInfo.getType(), contractInfo.getExtra());
                    }
                    break;
                case CONTRACT_PSM:
                    PSM psm = PSM.genInstance(contractInfo, tronChainHelper, iContractsHelper, psmTotalData, psmSigMap);
                    if (ObjectUtil.isNotNull(psm)) {
                        psmContracts.add(contractInfo.getAddress());
                        iContractsHelper.addContract(psm);
                    } else {
                        log.error("Fail to create instance for address: {}, type: {}, extra: {}", contractInfo.getAddress(),
                                contractInfo.getType(), contractInfo.getExtra());
                    }
                    break;
                case STABLE_SWAP_POOL:
                    NewCurvePool instance = NewCurvePool.genInstance(contractInfo, tronChainHelper,
                            iContractsHelper, null);
                    if (ObjectUtil.isNotNull(instance)) {
                        if (instance.getBaseCoinsCount() == 2) {
                            instance.setSigMap(curve2PoolSigMap);
                        } else if (instance.getBaseCoinsCount() == 3) {
                            instance.setSigMap(curve3PoolSigMap);
                        }
                        iContractsHelper.addContract(instance);
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
            iContractsHelper.addContract(iContractFactory.getBaseContract());
        }
        return true;
    }

    private void updatePsmTotalData(IContractsHelper iContractsHelper) {
        if (psmTotalData.isFinishInit() || psmContracts.size() == 0) {
            return;
        }
        for (String contract : psmContracts) {
            BaseContract baseContract = iContractsHelper.getContract(contract);
            if (ObjectUtil.isNull(baseContract) || !baseContract.isReady()) {
                return;
            }
        }
        PSM psm = (PSM) iContractsHelper.getContract(psmContracts.get(0));
        BigInteger[] totalInfos = psm.getTotalInfos();
        for (String contract : psmContracts) {
            PSM item = (PSM) iContractsHelper.getContract(contract);
            item.updateTotalInfos(totalInfos);
        }
        psmTotalData.setTotalMaxSwapUSDD(totalInfos[0]);
        psmTotalData.setTotalSwappedUSDD(totalInfos[1]);
        psmTotalData.setFinishInit(true);
    }

    public void resetPsmTotalDataState() {
        psmTotalData.setFinishInit(false);
    }


    public Pair<Integer, List<String>> updateMirrorContracts(IContractsHelper iContractsHelper, boolean firstFinishLoadData) {
        updatePsmTotalData(iContractsHelper);
        int addContracts = 0;
        List<String> subContractAddrs = new ArrayList<>();
        for (String addr : this.contractFactoryHashMap.keySet()) {
            IContractFactory iContractFactory = this.contractFactoryHashMap.get(addr);
            BaseFactory baseContract = iContractFactory.getBaseContract();
            if (!baseContract.isReady() || baseContract.hasFinishLoadSubContract()) {
                continue;
            }
            if (!iContractsHelper.containsContract(addr)) {
                iContractsHelper.addContract(baseContract);
                addContracts++;
            }

            List<BaseContract> baseContractList = iContractFactory.getListContracts(cmPool);
            for (BaseContract baseContract1 : baseContractList) {
                if (iContractsHelper.containsContract(baseContract1.address)) {
                    continue;
                }
                iContractsHelper.addContract(baseContract1);
                addContracts++;
                if (firstFinishLoadData) {
                    subContractAddrs.add(baseContract1.getAddress());
                }
            }
            baseContract.resetLoadSubContractState();
        }
        if (addContracts > 0) {
            log.info("ContractFactoryManager: start updateMirrorContracts, add contracts:{}", addContracts);
        }
        return Pair.of(addContracts, subContractAddrs);
    }
}
