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
    private static final int INIT_FLAG_SUCCESS = 3;
    private long t0;
    private long t1;
    private long t2;
    private int initFlag;
    @Setter
    @Getter
    private CountDownLatch latch;

    public BaseContract(String address, ContractType type, String version, IChainHelper iChainHelper, IContractsHelper iContractsHelper,
                        final Map<String, String> sigMap) {
        super(address, type, version, iChainHelper, iContractsHelper, sigMap);
        this.initFlag = INIT_FLAG_INIT;
    }

    public abstract boolean initDataFromChain1();

    protected abstract void saveUpdateToCache();

    protected abstract HandleResult handleEvent1(String eventName, String[] topics, String data, HandleEventExtraData handleEventExtraData);

    private boolean isContractIncremental() {
        return true;
    }

    public abstract <T> T getStatus();

    public abstract String getVersion();

    public abstract BaseContract copySelf();

    public abstract <T> T handleSpecialRequest(String method, String params) throws Exception;

    @SuppressWarnings("unchecked")
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
            stateInfo.ready = true;
            initFlag = INIT_FLAG_SUCCESS;
        } else {
            stateInfo.ready = false;
            initFlag = INIT_FLAG_FAILED;
            log.warn("OMG Ready contract {}:{} receive pre kafka msg, contract time:{} kafka time:{}, " +
                    "contract need reload", type, address, t2, iContractEventWrap.getTimeStamp());
        }
    }

    protected void initFull(IContractEventWrap iContractEventWrap) {
        initFlag = INIT_FLAG_DOING;
        long eventTime = iContractEventWrap.getTimeStamp();
        if (eventTime < t0) {
            return;
        }
        stateInfo.ready = true;
        initFlag = INIT_FLAG_SUCCESS;
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
        if (!stateInfo.ready) {
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
        if (!stateInfo.ready) {
            return HandleResult.genHandleFailMessage(String.format("Contract%s, type:%s not Ready", address, type), HandleResult.CODE_NOT_READY);
        }

        if (iContractEventWrap.getTimeStamp() <= t2) {
            log.warn("OMG Ready contract {}:{} receive pre kafka msg, contract time:{} kafka time:{}, " +
                    "contract need reload", type, address, t2, iContractEventWrap.getTimeStamp());
            this.resetReloadData();
            return HandleResult.genHandleFailMessage("OMG receive pre kafka!!", HandleResult.CODE_PRE_LOG);
        }
        // Do handleEvent
        String eventName = getEventName(iContractEventWrap);
        String[] topics = iContractEventWrap.getTopics();
        String data = iContractEventWrap.getData();
        HandleEventExtraData handleEventExtraData = genEventExtraData(iContractEventWrap);
        try {
            wLock.lock();
            HandleResult res = handleEvent1(eventName, topics, data, handleEventExtraData);
            if (!res.result && res.code == HandleResult.CODE_HANDLE_FAIL) {
                this.resetReloadData();
            }
            return res;
        } finally {
            wLock.unlock();
        }
    }

    // 处理完后统一更新数据到存储。
    public void finishBatchKafka() {
        if (initFlag == INIT_FLAG_START || initFlag == INIT_FLAG_DOING) {
            initFlag = INIT_FLAG_SUCCESS;
            stateInfo.ready = true;
        }
        if (!stateInfo.ready) {
            initFlag = INIT_FLAG_INIT;
        }
        if (stateInfo.dirty) {
//            saveUpdateToCache();
            stateInfo.dirty = false;
        }
    }

    public void resetReloadData() {
        stateInfo.resetReloadData();
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
        public static final int CODE_SUCCESS = 0;
        public static final int CODE_HANDLE_FAIL = 1;
        public static final int CODE_NOT_READY = 2;
        public static final int CODE_PRE_LOG = 3;
        public static final int CODE_USELESS_LOG = 4;
        private boolean result;
        private String message;
        private String[] newTopic;
        private String newData;
        private int code;

        public boolean needToSendMessage() {
            if (!result || newTopic == null || newTopic.length == 0) {
                return false;
            }
            return true;
        }

        public static HandleResult genHandleFailMessage(String msg) {
            return HandleResult.builder().result(false).code(CODE_HANDLE_FAIL).message(msg).build();
        }

        public static HandleResult genHandleUselessMessage(String msg) {
            return HandleResult.builder().result(true).code(CODE_USELESS_LOG).message(msg).build();
        }

        public static HandleResult genHandleFailMessage(String msg, int code) {
            return HandleResult.builder().result(false).code(code).message(msg).build();
        }


        public static HandleResult genHandleSuccess() {
            return HandleResult.builder().result(true).code(CODE_SUCCESS).build();
        }

        public static HandleResult genHandleSuccessAndSend(String[] topics, String data) {
            return HandleResult.builder().result(true).code(CODE_SUCCESS).newTopic(topics).newData(data).build();
        }
    }
}
