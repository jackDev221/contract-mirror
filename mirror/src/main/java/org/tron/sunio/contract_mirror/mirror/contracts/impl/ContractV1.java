package org.tron.sunio.contract_mirror.mirror.contracts.impl;

import cn.hutool.core.util.ObjectUtil;
import org.tron.sunio.contract_mirror.event_decode.logdata.ContractEventLog;
import org.tron.sunio.contract_mirror.mirror.cache.CacheHandler;
import org.tron.sunio.contract_mirror.mirror.chainHelper.IChainHelper;
import org.tron.sunio.contract_mirror.mirror.consts.ContractMirrorConst;
import org.tron.sunio.contract_mirror.mirror.contracts.BaseContract;
import org.tron.sunio.contract_mirror.mirror.dao.ContractV1Data;
import org.tron.sunio.contract_mirror.mirror.enums.ContractType;


import java.math.BigInteger;
import java.util.Map;


public class ContractV1 extends BaseContract {

    private String tokenAddress;

    public ContractV1(String address, IChainHelper iChainHelper, String tokenAddress, final Map<String, String> sigMap) {
        super(address, ContractType.CONTRACT_V1, iChainHelper, sigMap);
        this.tokenAddress = tokenAddress;
    }

    @Override
    public boolean initDataFromChain1() {
        ContractV1Data v1Data = CacheHandler.v1Cache.getIfPresent(tokenAddress);
        if (ObjectUtil.isNull(v1Data)) {
            v1Data = new ContractV1Data();
            v1Data.setType(this.type);
            v1Data.setAddress(this.address);
            v1Data.setUsing(true);
        }
        String name = callContractString(ContractMirrorConst.EMPTY_ADDRESS, "name");
        String symbol = callContractString(ContractMirrorConst.EMPTY_ADDRESS, "symbol");
        long decimals = callContractU256(ContractMirrorConst.EMPTY_ADDRESS, "decimals").longValue();
        int kLast = callContractUint(ContractMirrorConst.EMPTY_ADDRESS, "kLast").intValue();
        BigInteger trxBalance = getBalance(address);
        v1Data.setName(name);
        v1Data.setSymbol(symbol);
        v1Data.setDecimals(decimals);
        v1Data.setKLast(kLast);
        v1Data.setTrxBalance(trxBalance);
        v1Data.setReady(false);
        CacheHandler.v1Cache.put(this.address, v1Data);
        isReady = false;
        return true;
    }

    @Override
    public void handleEvent(ContractEventLog contractEventLog) {
        super.handleEvent(contractEventLog);
        if (!isReady) {
            return;
        }
        // Do handleEvent
    }
}
