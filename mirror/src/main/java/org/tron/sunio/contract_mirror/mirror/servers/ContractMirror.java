package org.tron.sunio.contract_mirror.mirror.servers;

import cn.hutool.core.lang.Pair;
import cn.hutool.core.util.ObjectUtil;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.KafkaException;
import org.apache.kafka.common.PartitionInfo;
import org.apache.kafka.common.TopicPartition;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.tron.sunio.contract_mirror.mirror.chainHelper.BlockInfo;
import org.tron.sunio.contract_mirror.mirror.chainHelper.IChainHelper;
import org.tron.sunio.contract_mirror.mirror.config.KafkaConfig;
import org.tron.sunio.contract_mirror.mirror.contracts.BaseContract;
import org.tron.sunio.contract_mirror.mirror.contracts.ContractFactoryManager;
import org.tron.sunio.contract_mirror.mirror.config.ContractsMirrorConfig;
import org.tron.sunio.contract_mirror.mirror.contracts.IContractsHelper;
import org.tron.sunio.contract_mirror.mirror.contracts.events.ContractEventWrap;
import org.tron.sunio.contract_mirror.mirror.pool.CMPool;
import org.tron.sunio.contract_mirror.mirror.tools.TimeTool;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CountDownLatch;

@Service
@Slf4j
@Data
public class ContractMirror implements InitializingBean, IContractsHelper {
    private final int EVENT_HANDLE_PERIOD = 20;
    private final int KAFKA_READY_CHECK_INTERVAL = 500;
    private final int KAFKA_PULL_TIMEOUT = 2000;

    @Autowired
    private ContractFactoryManager contractFactoryManager;
    @Autowired
    private ContractsMirrorConfig config;

    @Autowired
    private IChainHelper tronChainHelper;

    @Autowired
    private KafkaConfig kafkaConfig;

    @Autowired
    private CMPool cmPool;

    @Autowired
    RouterServer routerServer;

    private List<String> preUpdatedContracts = new ArrayList<>();

    private boolean firstFinishLoadData = false;

    private KafkaConsumer<String, String> consumer;
    private KafkaProducer<String, String> producer;

    private ConcurrentMap<String, BaseContract> contractHashMap = new ConcurrentHashMap<>();

    private BlockInfo blockInfo;

    @Override
    public void afterPropertiesSet() throws Exception {
        initKafka();
        contractFactoryManager.initFactoryMap(config.getFactoryInfos(), this);
        log.info("L1 height:{}", tronChainHelper.blockNumber());
    }

    private void initKafka() {
        try {
            if (kafkaConfig.getConsumerEnable()) {
                Properties properties = kafkaConfig.consumerConfig();
                consumer = new KafkaConsumer<String, String>(properties);
                consumer.subscribe(kafkaConfig.getTopics());
                setOffsetToEnd(kafkaConfig.getTopics());
            }
            if (kafkaConfig.getProducerEnable()) {
                Properties properties1 = kafkaConfig.producerConfig();
                producer = new KafkaProducer<String, String>(properties1);
            }
        } catch (KafkaException e) {
            throw new RuntimeException(e);
        }
    }

    private void setOffsetToEnd(List<String> topics) {
        if (kafkaConfig.getConsumerEnable() && ObjectUtil.isNotNull(consumer)) {
            for (String topic : topics) {
                List<PartitionInfo> partitionInfos = consumer.partitionsFor(topic);
                List<TopicPartition> tp =new ArrayList<TopicPartition>();
                if (null != partitionInfos && partitionInfos.size() > 0) {
                    partitionInfos.forEach(partitionInfo -> {
                        tp.add(new TopicPartition(topic, partitionInfo.partition()));
                    });
                    consumer.assign(tp);
                    consumer.seekToEnd(tp);
                }
            }
        }
    }

    private boolean isNeedReload(String hash, long number, long timeStamp) {
        if (ObjectUtil.isNull(blockInfo)) {
            blockInfo = new BlockInfo(number, hash, timeStamp);
            return false;
        }
        if (blockInfo.getNumber() < number) {
            blockInfo.setHash(hash);
            blockInfo.setNumber(number);
            return false;
        }
        if (blockInfo.getNumber() > number) {
            log.warn("OMG local number:{}, receive number:{}, need to reload", blockInfo.getNumber(), number);
            blockInfo = null;
            return true;
        }
        if (!blockInfo.getHash().equalsIgnoreCase(hash)) {
            log.warn("OMG local hash:{}, receive hash:{}, need to reload", blockInfo.getHash(), hash);
            blockInfo = null;
            return true;
        }
        return false;
    }

    private void setReloadAllContract() {
        log.warn("OMG into setReloadAllContract");
        for (BaseContract baseContract : contractHashMap.values()) {
            baseContract.resetReloadData();
        }
        this.contractFactoryManager.resetPsmTotalDataState();
    }

    private ConsumerRecords<String, String> kafkaConsumerPoll(long millis) {
        ConsumerRecords<String, String> records = ConsumerRecords.empty();
        if (kafkaConfig.getConsumerEnable() && ObjectUtil.isNotNull(consumer)) {
            try {
                records = consumer.poll(Duration.ofMillis(millis));
            } catch (Exception e) {
                log.error("kafkaPull err:{}", e);
            }
        }
        return records;
    }

    private void kafkaConsumerCommit() {
        if (kafkaConfig.getConsumerEnable() && ObjectUtil.isNotNull(consumer)) {
            consumer.commitSync();
        }
    }

    private void kafkaProducerSend(String topic, String key, String record) {
        if (kafkaConfig.getProducerEnable() && ObjectUtil.isNotNull(producer)) {
            producer.send(new ProducerRecord<>(topic, key, record), (metadata, exception) -> {
                if (exception != null) {
                    exception.printStackTrace();
                    log.warn("EventSender send error, topic {}, key {}, exception {}",
                            topic, key, exception);
                }
            });
        }
    }

    private void kafkaProducerFlush() {
        if (kafkaConfig.getProducerEnable() && ObjectUtil.isNotNull(producer)) {
            producer.flush();
        }
    }

    private int getUnReadyCount() {
        int res = 0;
        for (String addr : contractHashMap.keySet()) {
            BaseContract baseContract = contractHashMap.get(addr);
            if (!baseContract.isReady()) {
                res++;
            }
        }
        return res;
    }

    @Scheduled(initialDelay = 5000, fixedDelay = EVENT_HANDLE_PERIOD)
    public void doTask() {
        try {
            int unReadyContract = getUnReadyCount();
            if (unReadyContract > 0) {
                CountDownLatch latch = cmPool.initCountDownLatch(unReadyContract);
                for (String addr : contractHashMap.keySet()) {
                    BaseContract baseContract = contractHashMap.get(addr);
                    if (!baseContract.isReady()) {
                        // not ready call data from chain
//                      baseContract.initDataFromChain();
                        baseContract.setLatch(latch);
                        cmPool.submitT(baseContract::initDataFromChainThread);
                    }
                }
                cmPool.waitFinish();
            }
            // 工程合约更新完毕，且需要添加子合约。
//            int addContractNum =
            Pair<Integer, List<String>> addRes = contractFactoryManager.updateMirrorContracts(this, firstFinishLoadData);
            boolean needSleep = false;
            ConsumerRecords<String, String> records = kafkaConsumerPoll(KAFKA_PULL_TIMEOUT);
            for (ConsumerRecord<String, String> record : records) {
                ContractEventWrap contractEventWrap = ContractEventWrap.getInstance(record.topic(), record.value(), config.getNetwork());
                if (ObjectUtil.isNull(contractEventWrap)) {
                    continue;
                }
                if (isNeedReload(contractEventWrap.getBlockHash(), contractEventWrap.getBlockNumber(), contractEventWrap.getTimeStamp())) {
                    setReloadAllContract();
                    needSleep = true;
                    break;
                }
                BaseContract baseContract = contractHashMap.get(contractEventWrap.getContractAddress());
                if (ObjectUtil.isNotNull(baseContract)) {
                    BaseContract.HandleResult result = baseContract.handleEvent(contractEventWrap);
                    if (result.needToSendMessage()) {
                        String key = String.format("%s_new", record.key());
                        kafkaProducerSend(kafkaConfig.getProducerTopic(), key,
                                contractEventWrap.updateAndToJson(result.getNewTopic(), result.getNewData()));
                    }
                }
            }
            kafkaConsumerCommit();
            kafkaProducerFlush();
            for (String addr : contractHashMap.keySet()) {
                BaseContract baseContract = contractHashMap.get(addr);
                baseContract.finishBatchKafka();
            }
            if (needSleep) {
                TimeTool.sleep(config.getBlockInterval());
            } else {
                if (!firstFinishLoadData && unReadyContract == 0 && addRes.getKey().intValue() == 0) {
                    this.routerServer.initRoutNodeMap(this.contractHashMap);
                    firstFinishLoadData = true;
                }

                if (firstFinishLoadData) {
                    // v1 v2
                    if (ObjectUtil.isNotNull(preUpdatedContracts) && preUpdatedContracts.size() > 0 && unReadyContract == 0) {
                        this.routerServer.addRoutNodeMap(this.contractHashMap, preUpdatedContracts);
                        preUpdatedContracts = new ArrayList<>();
                    }
                    List<String> addContractAddrs = addRes.getValue();
                    if (addContractAddrs.size() > 0) {
                        preUpdatedContracts.addAll(addContractAddrs);
                    }
                }

            }
        } catch (Exception e) {
            e.printStackTrace();
            log.warn("doTask error:{}", e);
        }

    }

    @Override
    public void addContract(BaseContract baseContract) {
        this.contractHashMap.put(baseContract.getAddress(), baseContract);
    }

    @Override
    public BaseContract getContract(String address) {
        BaseContract baseContract = this.contractHashMap.get(address);
        if (ObjectUtil.isNotNull(baseContract)) {
            baseContract = baseContract.copySelf();
        }
        return baseContract;
    }

    @Override
    public boolean containsContract(String address) {
        return this.contractHashMap.containsKey(address);
    }

    @Override
    public boolean isContractReady(String address) {
        if (!containsContract(address)) {
            return false;
        }
        BaseContract baseContract = this.contractHashMap.get(address);
        return baseContract.isReady();
    }

    @Override
    public long getBlockTime() {
        // seconds
        return blockInfo.getTimeStamp() / 1000;
    }

    @Override
    public String toString() {
        return "ContractMirror{" +
                "EVENT_HANDLE_PERIOD=" + EVENT_HANDLE_PERIOD +
                ", KAFKA_READY_CHECK_INTERVAL=" + KAFKA_READY_CHECK_INTERVAL +
                ", KAFKA_PULL_TIMEOUT=" + KAFKA_PULL_TIMEOUT +
                ", config=" + config +
                ", tronChainHelper=" + tronChainHelper +
                ", kafkaConfig=" + kafkaConfig +
                ", firstFinishLoadData=" + firstFinishLoadData +
                ", blockInfo=" + blockInfo +
                '}';
    }
}
