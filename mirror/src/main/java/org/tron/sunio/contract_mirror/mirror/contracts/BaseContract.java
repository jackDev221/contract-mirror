package org.tron.sunio.contract_mirror.mirror.contracts;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.tron.sunio.contract_mirror.mirror.cache.CacheHandler;
import org.tron.sunio.contract_mirror.mirror.chainHelper.IChainHelper;
import org.tron.sunio.contract_mirror.mirror.chainHelper.TriggerContractInfo;
import org.tron.sunio.contract_mirror.mirror.consts.ContractMirrorConst;
import org.tron.sunio.contract_mirror.mirror.enums.ContractType;
import org.tron.sunio.tronsdk.WalletUtil;
import org.web3j.abi.TypeReference;
import org.web3j.abi.datatypes.Address;
import org.web3j.abi.datatypes.Type;
import org.web3j.abi.datatypes.Uint;
import org.web3j.abi.datatypes.Utf8String;
import org.web3j.abi.datatypes.generated.Uint256;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

@Data
@Slf4j
public class BaseContract implements IContract {
    private static final int INIT_FLAG_INIT = -1;
    private static final int INIT_FLAG_START = 0;
    private static final int INIT_FLAG_DOING = 1;
    private static final int INIT_FLAG_FAILED = 2;
    private static final int INIT_FLAG_SUCCESS = 2;
    protected String address;
    protected ContractType type;
    protected boolean isReady;
    protected boolean isUsing;
    protected IChainHelper iChainHelper;
    protected boolean isAddExchangeContracts;
    private long t0;
    private long t1;
    private int initFlag;

    public BaseContract(String address, ContractType type, IChainHelper iChainHelper) {
        this.type = type;
        this.address = address;
        this.iChainHelper = iChainHelper;
        this.isUsing = true;
        this.initFlag = INIT_FLAG_INIT;
    }

    public boolean initDataFromChain() {
        log.info("Contract:{}, type:{} start init", this.address, this.type);
        t0 = System.currentTimeMillis();
        initFlag = INIT_FLAG_START;
        initDataFromChain1();
        t1 = System.currentTimeMillis();
        return false;
    }

    public void handleEvent() {
        if (initFlag == INIT_FLAG_START) {
            initFlag = INIT_FLAG_DOING;
        }
    }

    // 针对该批次kafka没有对应要消费的事件
    public void finishBatchKafka() {
        if (initFlag == INIT_FLAG_START) {
            initFlag = INIT_FLAG_SUCCESS;
        }
        if (initFlag == INIT_FLAG_SUCCESS){
            isReady = true;
        }else{
            initFlag = INIT_FLAG_INIT;
        }
    }

    @Override
    public ContractType getContractType() {
        return type;
    }

    @Override
    public boolean initDataFromChain1() {
        return false;
    }

    protected String callContractString(String from, String method) {
        List<Type> inputParameters = new ArrayList<>();
        List<TypeReference<?>> outputParameters = new ArrayList<>();
        outputParameters.add(new TypeReference<Utf8String>() {
        });
        TriggerContractInfo triggerContractInfo = new TriggerContractInfo(
                this.getAddress(),
                from,
                method,
                inputParameters,
                outputParameters
        );
        List<Type> results = this.iChainHelper.triggerConstantContract(triggerContractInfo);
        return results.get(0).getValue().toString();
    }

    protected BigInteger callContractU256(String from, String method) {
        List<Type> inputParameters = new ArrayList<>();
        List<TypeReference<?>> outputParameters = new ArrayList<>();
        outputParameters.add(new TypeReference<Uint256>() {
        });
        TriggerContractInfo triggerContractInfo = new TriggerContractInfo(
                this.getAddress(),
                from,
                method,
                inputParameters,
                outputParameters
        );
        List<Type> results = this.iChainHelper.triggerConstantContract(triggerContractInfo);
        return (BigInteger) results.get(0).getValue();
    }

    protected BigInteger callContractUint(String from, String method) {
        List<Type> inputParameters = new ArrayList<>();
        List<TypeReference<?>> outputParameters = new ArrayList<>();
        outputParameters.add(new TypeReference<Uint>() {
        });
        TriggerContractInfo triggerContractInfo = new TriggerContractInfo(
                this.getAddress(),
                from,
                method,
                inputParameters,
                outputParameters
        );
        List<Type> results = this.iChainHelper.triggerConstantContract(triggerContractInfo);
        return (BigInteger) results.get(0).getValue();
    }

    protected Address callContractAddress(String from, String method) {
        List<Type> inputParameters = new ArrayList<>();
        List<TypeReference<?>> outputParameters = new ArrayList<>();
        outputParameters.add(new TypeReference<Address>() {
        });
        TriggerContractInfo triggerContractInfo = new TriggerContractInfo(
                this.getAddress(),
                from,
                method,
                inputParameters,
                outputParameters
        );
        List<Type> results = this.iChainHelper.triggerConstantContract(triggerContractInfo);
        return (Address) results.get(0).getValue();
    }

    protected BigInteger getBalance(String address) {
        return iChainHelper.balance(address);
    }
}
