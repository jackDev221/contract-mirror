package org.tron.sunio.contract_mirror.event_decode.logdata;

import com.google.gson.JsonObject;
import lombok.Data;
import org.tron.sunio.contract_mirror.event_decode.utils.GsonUtil;

@Data
public class ContractEventLog {
    private long timeStamp;
    private String triggerName;
    private String uniqueId;
    private String transactionId;
    private String contractAddress;
    private String callerAddress;
    private String originAddress;
    private String creatorAddress;
    private long blockNumber;
    private String blockHash;
    private boolean removed;
    private long latestSolidifiedBlockNumber;
    private RawData rawData;
    private String eventSignature;
    private String eventSignatureFull;
    private String eventName;
    private JsonObject topicMap;
    private JsonObject dataMap;

    public String updateAndToJson(String[] topicList, String data) {
        this.rawData.setTopics(topicList);
        this.rawData.setData(data);
        this.uniqueId = String.format("%s_new", uniqueId);
        return GsonUtil.objectToGson(this);
    }
}
