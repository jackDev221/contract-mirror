package org.tron.sunio.contract_mirror.mirror.contracts.factory;

import cn.hutool.core.util.ObjectUtil;
import lombok.extern.slf4j.Slf4j;
import org.tron.sunio.contract_mirror.mirror.chainHelper.IChainHelper;
import org.tron.sunio.contract_mirror.mirror.contracts.BaseContract;
import org.tron.sunio.contract_mirror.mirror.contracts.IContractsHelper;
import org.tron.sunio.contract_mirror.mirror.contracts.IContractFactory;
import org.tron.sunio.contract_mirror.mirror.enums.ContractType;
import org.tron.sunio.contract_mirror.mirror.pool.CMPool;
import org.tron.sunio.contract_mirror.mirror.pool.process.in.SwapFactoryExIn;
import org.tron.sunio.contract_mirror.mirror.pool.process.out.BaseProcessOut;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Slf4j
public class BaseFactory extends BaseContract implements IContractFactory {
    protected boolean hasNewContract;
    protected List<BaseContract> newSubContracts = new ArrayList<>();
    private List<Integer> unFinshLoadSub = new ArrayList<>();

    public BaseFactory(String address, ContractType type, IChainHelper iChainHelper, IContractsHelper iContractsHelper, Map<String, String> sigMap) {
        super(address, type, iChainHelper, iContractsHelper, sigMap);
    }

    public boolean hasFinishLoadSubContract(){
        return !hasNewContract && isAddExchangeContracts;
    }

    @Override
    public boolean initDataFromChain1() {
        return false;
    }

    @Override
    public void updateBaseInfo(boolean isUsing, boolean isReady, boolean isAddExchangeContracts) {

    }

    @Override
    protected void saveUpdateToCache() {

    }

    @Override
    protected HandleResult handleEvent1(String eventName, String[] topics, String data, HandleEventExtraData handleEventExtraData) {
        return null;
    }

    @Override
    public <T> T getStatus() {
        return null;
    }

    @Override
    public String getVersion() {
        return null;
    }

    @Override
    public <T> T handleSpecialRequest(String method, String params) throws Exception {
        return null;
    }

    @Override
    public BaseFactory getBaseContract() {
        return this;
    }

    @Override
    public List<BaseContract> getListContracts(CMPool cmPool) {
        return null;
    }

    protected List<BaseProcessOut> getListContractsBase(CMPool cmPool, int baseCount) {
        log.info("Into BaseFactory: getListContractsBase:baseCount:{} unFinshLoadSub:{}", baseCount, unFinshLoadSub.size());
        List<BaseProcessOut> result = new ArrayList<>();
        if (baseCount <= 0 && unFinshLoadSub.size() == 0) {
            return Collections.emptyList();
        }
        List<Integer> unfinished = new ArrayList<>();
        if (unFinshLoadSub.size() == 0) {
            cmPool.initCountDownLatch(baseCount);
            for (long i = 0; i < baseCount; i++) {
                addTaskToPool(cmPool, i);
            }
            cmPool.waitFinish();
            for (long i = 0; i < baseCount; i++) {
                String key = this.type.getDesc() + "_" + i;
                getSubThreadReult(cmPool, result, unfinished, (int) i, key);
            }
        } else {
            cmPool.initCountDownLatch(unFinshLoadSub.size());
            for (Integer i : unFinshLoadSub) {
                addTaskToPool(cmPool, i);
            }
            cmPool.waitFinish();
            for (Integer i : unFinshLoadSub) {
                String key = this.type.getDesc() + "_" + i;
                getSubThreadReult(cmPool, result, unfinished, (int) i, key);
            }
        }
        int finishCount = cmPool.getResultSize();
        log.info("BaseFactory: getListContractsBase Result: finishCount:{} unfinished:{}", finishCount, unfinished.size());
        unFinshLoadSub.clear();
        unFinshLoadSub.addAll(unfinished);
        return result;
    }

    private void getSubThreadReult(CMPool cmPool, List<BaseProcessOut> result, List<Integer> unfinished, int i, String key) {
        BaseProcessOut out = cmPool.getProcessRes(key);
        if (ObjectUtil.isNull(out)) {
            unfinished.add(i);
            return;
        }
        result.add(out);
    }

    private void addTaskToPool(CMPool cmPool, long i) {
        String key = this.type.getDesc() + "_" + i;
        SwapFactoryExIn in = new SwapFactoryExIn();
        in.setId((int) i);
        in.setOutKey(key);
        in.setAddress(this.address);
        in.setProcessType(this.type.getDesc());
        cmPool.submit(in);
    }

    public void resetLoadSubContractState() {
        setAddExchangeContracts(unFinshLoadSub.size() == 0);
    }
}
