package org.tron.sunio.contract_mirror.mirror.contracts;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.tron.sunio.contract_mirror.mirror.chainHelper.IChainHelper;
import org.tron.sunio.contract_mirror.mirror.enums.ContractType;

import java.math.BigInteger;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

@Data
@Slf4j
public class ContractObj implements IContract {
    protected final ReadWriteLock rwlock = new ReentrantReadWriteLock();
    protected final Lock rlock = rwlock.readLock();
    protected final Lock wlock = rwlock.writeLock();
    protected String address;
    protected ContractType type;
    protected String version;
    protected boolean isReady;
    protected boolean isUsing;
    protected boolean isAddExchangeContracts;
    protected boolean isDirty;
    protected Map<String, String> sigMap;
    protected IChainHelper iChainHelper;
    @Getter
    @Setter
    protected IContractsHelper iContractsHelper;

    public ContractObj(String address, ContractType type, String version, IChainHelper iChainHelper, IContractsHelper iContractsHelper,
                       final Map<String, String> sigMap) {
        this.type = type;
        this.address = address;
        this.version = version;
        this.iChainHelper = iChainHelper;
        this.sigMap = sigMap;
        this.isUsing = true;
        this.iContractsHelper = iContractsHelper;
    }

    protected BigInteger getBalance(String address) {
        return iChainHelper.balance(address);
    }

}
