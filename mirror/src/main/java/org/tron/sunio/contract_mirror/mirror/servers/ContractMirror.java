package org.tron.sunio.contract_mirror.mirror.servers;

import cn.hutool.core.util.ObjectUtil;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.KafkaException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.tron.sunio.contract_mirror.event_decode.logdata.ContractEventLog;
import org.tron.sunio.contract_mirror.event_decode.LogDecode;
import org.tron.sunio.contract_mirror.mirror.chainHelper.BlockInfo;
import org.tron.sunio.contract_mirror.mirror.config.KafkaConfig;
import org.tron.sunio.contract_mirror.mirror.contracts.BaseContract;
import org.tron.sunio.contract_mirror.mirror.contracts.ContractFactoryManager;
import org.tron.sunio.contract_mirror.mirror.config.ContractsMirrorConfig;
import org.tron.sunio.contract_mirror.mirror.tools.TimeTool;

import java.time.Duration;
import java.util.HashMap;
import java.util.Properties;

@Service
@Slf4j
@Data
public class ContractMirror implements InitializingBean {
    @Autowired
    private ContractFactoryManager contractFactoryManager;
    @Autowired
    private ContractsMirrorConfig config;

    @Autowired
    private KafkaConfig kafkaConfig;

    private KafkaConsumer<String, String> consumer;

    private HashMap<String, BaseContract> contractHashMap = new HashMap<>();

    private BlockInfo blockInfo;

    @Override
    public void afterPropertiesSet() throws Exception {
        initKafka();
        contractFactoryManager.initFactoryMap(config.getListContractFactory());
        doTask();
    }

    private void initKafka() {
        try {
            Properties properties = kafkaConfig.defaultConfig();
            consumer = new KafkaConsumer<String, String>(properties);
            consumer.subscribe(kafkaConfig.getTopics());
        } catch (KafkaException e) {
            throw new RuntimeException(e);
        }
    }

    private boolean isNeedReload(String hash, long number) {
        if (ObjectUtil.isNull(blockInfo)) {
            blockInfo = new BlockInfo(number, hash);
            return false;
        }
        if (blockInfo.getNumber() < number) {
            blockInfo.setHash(hash);
            blockInfo.setNumber(number);
            return false;
        }
        if (blockInfo.getNumber() > number) {
            blockInfo = null;
            return true;
        }
        if (!blockInfo.getHash().equalsIgnoreCase(hash)) {
            blockInfo = null;
            return true;
        }
        return false;
    }

    private void setReloadAllContract() {
        for (BaseContract baseContract : contractHashMap.values()) {
            baseContract.setReady(false);
            baseContract.setAddExchangeContracts(false);
        }
    }

    private void doTask() {
        while (true) {
           try{
               for (String addr : contractHashMap.keySet()) {
                   BaseContract baseContract = contractHashMap.get(addr);
                   if (!baseContract.isReady()) {
                       // not ready call data from chain
                       baseContract.initDataFromChain();
                   }
               }
               // 工程合约更新完毕，且需要添加子合约。
               contractFactoryManager.updateMirrorContracts();
               boolean needSleep = false;
               ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(200L));
               for (ConsumerRecord<String, String> record : records) {
                   ContractEventLog contractEventLog = LogDecode.decode(record.value());
                   if (isNeedReload(contractEventLog.getBlockHash(), contractEventLog.getBlockNumber())) {
                       setReloadAllContract();
                       needSleep = true;
                       break;
                   }
                   BaseContract baseContract = contractHashMap.get(contractEventLog.getContractAddress());
                   if (ObjectUtil.isNotNull(baseContract)) {
                       baseContract.handleEvent(contractEventLog);
                   }
               }
               consumer.commitSync();
               if(needSleep){
                   TimeTool.sleep(config.getBlockInterval());
               }
               for (String addr : contractHashMap.keySet()) {
                   BaseContract baseContract = contractHashMap.get(addr);
                   if (!baseContract.isReady()) {
                       // not ready call data from chain
                       baseContract.finishBatchKafka();
                   }
               }


           }catch (Exception e){
               log.warn("doTask error:{}", e.toString());
           }
        }
    }
}
