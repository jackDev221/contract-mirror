package org.tron.sunio.contract_mirror.mirror.contracts;

import cn.hutool.core.util.ObjectUtil;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.tron.sunio.contract_mirror.event_decode.events.EventUtils;
import org.tron.sunio.contract_mirror.mirror.chainHelper.IChainHelper;
import org.tron.sunio.contract_mirror.mirror.contracts.events.IContractEventWrap;
import org.tron.sunio.contract_mirror.mirror.enums.ContractType;
import org.web3j.abi.EventValues;

import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

import static org.tron.sunio.contract_mirror.mirror.consts.ContractMirrorConst.METHOD_STATUS;
import static org.tron.sunio.contract_mirror.mirror.consts.ContractMirrorConst.METHOD_VERSION;

@Slf4j
public abstract class BaseContract extends ContractObj {
    private static final int INIT_FLAG_INIT = -1;
    private static final int INIT_FLAG_START = 0;
    private static final int INIT_FLAG_DOING = 1;
    private static final int INIT_FLAG_FAILED = 2;
    private static final int INIT_FLAG_SUCCESS = 2;
    private long t0;
    private long t1;
    private long t2;
    private int initFlag;
    @Setter
    @Getter
    private CountDownLatch latch;

    public BaseContract(String address, ContractType type, IChainHelper iChainHelper, IContractsHelper iContractsHelper,
                        final Map<String, String> sigMap) {
        super(address, type, iChainHelper, iContractsHelper, sigMap);
        this.initFlag = INIT_FLAG_INIT;
    }

    public abstract boolean initDataFromChain1();

    public abstract void updateBaseInfo(boolean isUsing, boolean isReady, boolean isAddExchangeContracts);

    protected abstract void saveUpdateToCache();

    protected abstract HandleResult handleEvent1(String eventName, String[] topics, String data, HandleEventExtraData handleEventExtraData);

    private boolean isContractIncremental() {
        return false;
    }

    public abstract <T> T getStatus();

    public abstract String getVersion();

    public abstract <T> T handleSpecialRequest(String method, String params) throws Exception;

    public <T> T handRequest(String method, String params) throws Exception {
        if (method.equalsIgnoreCase(METHOD_STATUS)) {
            return getStatus();
        }
        if (method.equalsIgnoreCase(METHOD_VERSION)) {
            return (T) getVersion();
        }
        return handleSpecialRequest(method, params);
    }


    public boolean initDataFromChainThread() {
        try {
            log.info("Contract:{}, type:{} start init", this.address, this.type);
            t0 = System.currentTimeMillis();
            initFlag = INIT_FLAG_START;
            initDataFromChain1();
            t1 = System.currentTimeMillis();
            t2 = t1 + (t1 - t0);
            log.info("Contract:{}, type:{} finish  function initDataFromChainThread t0:{}, t1:{}, t2:{}", address, type, t0, t1, t2);
        } catch (Exception e) {
            log.error("Contract:{} type:{}, failed at function initDataFromChain:{}, init failed", address, type, e.toString());
            initFlag = INIT_FLAG_FAILED;
            return false;
        } finally {
            if (ObjectUtil.isNotNull(latch)) {
                latch.countDown();
            }
        }
        return true;
    }

    public boolean initDataFromChain() {
        try {
            log.info("Contract:{}, type:{} start init", this.address, this.type);
            t0 = System.currentTimeMillis();
            initFlag = INIT_FLAG_START;
            initDataFromChain1();
            t1 = System.currentTimeMillis();
            t2 = t1 + (t1 - t0);
        } catch (Exception e) {
            log.error("Contract:{} type:{}, failed at function initDataFromChain:{}, init failed", address, type, e.toString());
            initFlag = INIT_FLAG_FAILED;
            return false;
        }
        return true;
    }

    protected void initIncremental(IContractEventWrap iContractEventWrap) {
        initFlag = INIT_FLAG_DOING;
        long eventTime = iContractEventWrap.getTimeStamp();
        if (eventTime < t0) {
            return;
        }
        if (eventTime > t2) {
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
            log.warn("Wrong log no topic, id: {}", iContractEventWrap.getUniqueId());
            return null;
        }
        return sigMap.getOrDefault(topics[0], "");
    }

    public HandleResult handleEvent(IContractEventWrap iContractEventWrap) {
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
            return HandleResult.genHandleFailMessage(String.format("Contract%s, type:%s not Ready", address, type));
        }

        if (iContractEventWrap.getTimeStamp() <= t2) {
            log.error("OMG Ready contract {}:{}receive pre kafka msg, contract time:{} kafka time:{}, " +
                    "contract need reload", type, address, t2, iContractEventWrap.getTimeStamp());
            this.resetReloadData();
            return HandleResult.genHandleFailMessage("OMG receive pre kafka!!");
        }
        // Do handleEvent
        String eventName = getEventName(iContractEventWrap);
        String[] topics = iContractEventWrap.getTopics();
        String data = iContractEventWrap.getData();
        HandleEventExtraData handleEventExtraData = genEventExtraData(iContractEventWrap);
        return handleEvent1(eventName, topics, data, handleEventExtraData);
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
//            saveUpdateToCache();
            isDirty = false;
        }
    }

    public void resetReloadData() {
        isReady = false;
        isAddExchangeContracts = false;
        isDirty = true;
        this.updateBaseInfo(isUsing, isReady, isAddExchangeContracts);
    }

    protected EventValues getEventValue(String eventName, String eventBody, String[] topics, String data, String id) {
        EventValues values = EventUtils.getEventValue(eventBody, Arrays.asList(topics), data, false);
        if (ObjectUtil.isNull(values)) {
            log.error("Contract:{}, type:{} handEvent:{}, id:{}  failed!!", address, type, eventName, id);
            return null;
        }
        return values;
    }

    private HandleEventExtraData genEventExtraData(IContractEventWrap iContractEventWrap) {
        return HandleEventExtraData.builder()
                .timeStamp(iContractEventWrap.getTimeStamp() / 1000)
                .UniqueId(iContractEventWrap.getUniqueId())
                .build();

    }

    @Data
    @Builder
    public static class HandleEventExtraData {
        private long timeStamp;
        private String UniqueId;
    }

    @Data
    @Builder
    public static class HandleResult {
        private boolean result;
        private String message;
        private String[] newTopic;
        private String newData;

        public boolean needToSendMessage() {
            if (!result || newTopic == null || newTopic.length == 0) {
                return false;
            }
            return true;
        }

        public static HandleResult genHandleFailMessage(String msg) {
            return HandleResult.builder().result(false).message(msg).build();
        }

        public static HandleResult genHandleSuccess() {
            return HandleResult.builder().result(true).build();
        }

        public static HandleResult genHandleSuccessAndSend(String[] topics, String data) {
            return HandleResult.builder().result(true).newTopic(topics).newData(data).build();
        }
    }
}
