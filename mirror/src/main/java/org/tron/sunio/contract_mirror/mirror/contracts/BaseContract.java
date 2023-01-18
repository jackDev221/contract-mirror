package org.tron.sunio.contract_mirror.mirror.contracts;

import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.tron.sunio.contract_mirror.mirror.chainHelper.IChainHelper;
import org.tron.sunio.contract_mirror.mirror.chainHelper.TriggerContractInfo;
import org.tron.sunio.contract_mirror.mirror.contracts.events.IContractEventWrap;
import org.tron.sunio.contract_mirror.mirror.db.IDbHandler;
import org.tron.sunio.contract_mirror.mirror.enums.ContractType;
import org.tron.sunio.contract_mirror.mirror.tools.EthUtil;
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
import java.util.Map;

@Data
@Slf4j
public abstract class BaseContract implements IContract {
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
    protected boolean isDirty;
    private long t0;
    private long t1;
    private long t2;
    private int initFlag;
    protected Map<String, String> sigMap;
    protected IDbHandler iDbHandler;

    public abstract boolean initDataFromChain1();

    public abstract void updateBaseInfo(boolean isUsing, boolean isReady, boolean isAddExchangeContracts);

    protected abstract void saveUpdateToCache();

    protected abstract void handleEvent1(String eventName, String[] topics, String data, HandleEventExtraData handleEventExtraData);

    private boolean isContractIncremental() {
        return false;
    }

    public BaseContract(String address, ContractType type, IChainHelper iChainHelper, IDbHandler iDbHandler,
                        final Map<String, String> sigMap) {
        this.type = type;
        this.address = address;
        this.iChainHelper = iChainHelper;
        this.iDbHandler = iDbHandler;
        this.sigMap = sigMap;
        this.isUsing = true;
        this.initFlag = INIT_FLAG_INIT;
    }

    public boolean initDataFromChain() {
        log.info("Contract:{}, type:{} start init", this.address, this.type);
        t0 = System.currentTimeMillis();
        initFlag = INIT_FLAG_START;
        initDataFromChain1();
        t1 = System.currentTimeMillis();
        t2 = t1 + (t1 - t0);
        return false;
    }

    protected void initIncremental(IContractEventWrap iContractEventWrap) {
        initFlag = INIT_FLAG_DOING;
        long eventTime = iContractEventWrap.getTimeStamp();
        if (eventTime < t0) {
            return;
        }
        if (eventTime <= t2) {
            isReady = true;
            updateBaseInfo(isUsing, isReady, isAddExchangeContracts);
            initFlag = INIT_FLAG_SUCCESS;
        } else {
            isReady = false;
            initFlag = INIT_FLAG_FAILED;
        }
    }

    protected void initFull(IContractEventWrap iContractEventWrap) {
        initFlag = INIT_FLAG_DOING;
        long eventTime = iContractEventWrap.getTimeStamp();
        if (eventTime < t0) {
            return;
        }
        isReady = true;
        initFlag = INIT_FLAG_SUCCESS;
        updateBaseInfo(isUsing, isReady, isAddExchangeContracts);
    }

    protected String getEventName(IContractEventWrap iContractEventWrap) {
        String[] topics = iContractEventWrap.getTopics();
        if (topics == null || topics.length <= 0) {
            log.warn("Wrong log no topic, id:{}", iContractEventWrap.getUniqueId());
            return null;
        }
        return sigMap.getOrDefault(topics[0], "");
    }

    public void handleEvent(IContractEventWrap iContractEventWrap) {
        if (!isReady) {
            // 过滤中间需要重新init和初始化失败
            if (initFlag == INIT_FLAG_START || initFlag == INIT_FLAG_DOING) {
                initFlag = INIT_FLAG_DOING;
                if (isContractIncremental()) {
                    initIncremental(iContractEventWrap);
                } else {
                    initFull(iContractEventWrap);
                }
            }
        }
        if (!isReady) {
            return;
        }
        // Do handleEvent
        String eventName = getEventName(iContractEventWrap);
        String[] topics = iContractEventWrap.getTopics();
        String data = iContractEventWrap.getData();
        HandleEventExtraData handleEventExtraData = genEventExtraData(iContractEventWrap);
        handleEvent1(eventName, topics, data, handleEventExtraData);
    }

    // 处理完后统一更新数据到存储。
    public void finishBatchKafka() {
        if (initFlag == INIT_FLAG_START || initFlag == INIT_FLAG_DOING) {
            initFlag = INIT_FLAG_SUCCESS;
            isReady = true;
            updateBaseInfo(isUsing, isReady, isAddExchangeContracts);
        }
        if (!isReady) {
            initFlag = INIT_FLAG_INIT;
        }
        if (isDirty) {
            saveUpdateToCache();
        }
    }

    @Override
    public ContractType getContractType() {
        return type;
    }


    protected String callContractString(String from, String method) {
        List<Type> inputParameters = new ArrayList<>();
        List<TypeReference<?>> outputParameters = new ArrayList<>();
        outputParameters.add(new TypeReference<Utf8String>() {
        });
        TriggerContractInfo triggerContractInfo = new TriggerContractInfo(
                from,
                this.getAddress(),
                method,
                inputParameters,
                outputParameters
        );
        List<Type> results = this.iChainHelper.triggerConstantContract(triggerContractInfo);
        if (results.size() == 0) {
            log.error("Get contract:{} type:{} , function:{} result len is zero", this.address, this.type, method);
            return "";
        }
        return results.get(0).getValue().toString();
    }

    protected BigInteger callContractU256(String from, String method) {
        List<Type> inputParameters = new ArrayList<>();
        List<TypeReference<?>> outputParameters = new ArrayList<>();
        outputParameters.add(new TypeReference<Uint256>() {
        });
        TriggerContractInfo triggerContractInfo = new TriggerContractInfo(
                from,
                this.getAddress(),
                method,
                inputParameters,
                outputParameters
        );
        List<Type> results = this.iChainHelper.triggerConstantContract(triggerContractInfo);
        if (results.size() == 0) {
            log.error("Get contract:{} type:{} , function:{} result len is zero", this.address, this.type, method);
            return BigInteger.ZERO;
        }
        return (BigInteger) results.get(0).getValue();
    }

    protected long callContractUint(String from, String method) {
        List<Type> inputParameters = new ArrayList<>();
        List<TypeReference<?>> outputParameters = new ArrayList<>();
        outputParameters.add(new TypeReference<Uint>() {
        });
        TriggerContractInfo triggerContractInfo = new TriggerContractInfo(
                from,
                this.getAddress(),
                method,
                inputParameters,
                outputParameters
        );
        List<Type> results = this.iChainHelper.triggerConstantContract(triggerContractInfo);
        if (results.size() == 0) {
            log.error("Get contract:{} type:{} , function:{} result len is zero", this.address, this.type, method);
            return 0;
        }
        return (long) results.get(0).getValue();
    }

    protected Address callContractAddress(String from, String method) {
        List<Type> inputParameters = new ArrayList<>();
        List<TypeReference<?>> outputParameters = new ArrayList<>();
        outputParameters.add(new TypeReference<Address>() {
        });
        TriggerContractInfo triggerContractInfo = new TriggerContractInfo(
                from,
                this.getAddress(),
                method,
                inputParameters,
                outputParameters
        );
        List<Type> results = this.iChainHelper.triggerConstantContract(triggerContractInfo);
        if (results.size() == 0) {
            log.error("Get contract:{} type:{} , function:{} result len is zero", this.address, this.type, method);
            return Address.DEFAULT;
        }
        return new Address(EthUtil.addHexPrefix((String) results.get(0).getValue()));
    }

    protected BigInteger tokenBalance(String from, String contract) {
        List<Type> inputParameters = new ArrayList<>();
        inputParameters.add(new Address(WalletUtil.ethAddressHex(from)));
        List<TypeReference<?>> outputParameters = new ArrayList<>();
        outputParameters.add(new TypeReference<Uint256>() {
        });
        TriggerContractInfo triggerContractInfo = new TriggerContractInfo(
                from,
                contract,
                "balanceOf",
                inputParameters,
                outputParameters
        );
        List<Type> results = this.iChainHelper.triggerConstantContract(triggerContractInfo);
        if (results.size() == 0) {
            log.error("Get account:{}, token:{} , function:balanceOf result len is zero", from, contract);
            return BigInteger.ZERO;
        }
        return (BigInteger) results.get(0).getValue();
    }

    protected BigInteger getBalance(String address) {
        return iChainHelper.balance(address);
    }

    private HandleEventExtraData genEventExtraData(IContractEventWrap iContractEventWrap) {
        return HandleEventExtraData.builder()
                .timeStamp(iContractEventWrap.getTimeStamp())
                .build();

    }

    @Data
    @Builder
    public static class HandleEventExtraData {
        private long timeStamp;
    }
}
