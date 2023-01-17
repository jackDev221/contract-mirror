package org.tron.sunio.contract_mirror.event_decode.events;

import cn.hutool.core.lang.Assert;
import org.junit.jupiter.api.Test;
import org.tron.sunio.contract_mirror.event_decode.LogDecode;
import org.tron.sunio.contract_mirror.event_decode.logdata.ContractEventLog;
import org.tron.sunio.contract_mirror.event_decode.logdata.ContractLog;
import org.tron.sunio.contract_mirror.event_decode.utils.GsonUtil;
import org.web3j.abi.datatypes.generated.StaticArray1;
import org.web3j.abi.datatypes.generated.StaticArray2;
import org.web3j.abi.datatypes.generated.Uint32;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class LogDecodeTest {
    @Test
    public void testContractLog() {
        String jsonString = "{\"timeStamp\":1670964258000,\"triggerName\":\"contractLogTrigger\",\"uniqueId\":\"902f5e73b3bcd7b089d9bde02791d249bc546a24304b506522de213aa0cad7b2_4\",\"transactionId\":\"902f5e73b3bcd7b089d9bde02791d249bc546a24304b506522de213aa0cad7b2\",\"contractAddress\":\"TC1GhhC5iGFLuuUthriuUu183P8YWPmQsK\",\"callerAddress\":\"\",\"originAddress\":\"TQooBX9o8iSSprLWW96YShBogx7Uwisuim\",\"creatorAddress\":\"TKWJdrQkqHisa1X8HUdHEfREvTzw4pMAaY\",\"blockNumber\":46791292,\"blockHash\":\"0000000002c9fa7c1f450f0eefdc99588fdb1708f5b9e143cf9702281facde2d\",\"removed\":false,\"latestSolidifiedBlockNumber\":46791273,\"rawData\":{\"address\":\"16542c2f3fca9bf358b60329979af79883be9682\",\"topics\":[\"d78ad95fa46c994b6551d0da85fc275fe613ce37657fb8d5e3d130840159d822\",\"0000000000000000000000006e0617948fe030a7e4970f8389d4ad295f249b7e\",\"000000000000000000000000a2c2426d23bb43809e6eba1311afddde8d45f5d8\"],\"data\":\"0000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000001bd944be000000000000000000000000000000000000000000000000000004cf3961ace70000000000000000000000000000000000000000000000000000000000000000\"},\"topicList\":[\"d78ad95fa46c994b6551d0da85fc275fe613ce37657fb8d5e3d130840159d822\",\"0000000000000000000000006e0617948fe030a7e4970f8389d4ad295f249b7e\",\"000000000000000000000000a2c2426d23bb43809e6eba1311afddde8d45f5d8\"],\"data\":\"0000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000001bd944be000000000000000000000000000000000000000000000000000004cf3961ace70000000000000000000000000000000000000000000000000000000000000000\"}";
        ContractLog eventLog = LogDecode.decodeContractLog(jsonString);
        String gsonString = GsonUtil.objectToGson(eventLog);
        Assert.equals(jsonString, gsonString);
    }

    @Test
    public void testContractEventLog() {
        String jsonString = "{\"timeStamp\":1670958264000,\"triggerName\":\"contractEventTrigger\",\"uniqueId\":\"262001121bd7b08fd13a66d9d76ed43aac6e6c43eb6832e6d782e294d1061be5_4\",\"transactionId\":\"262001121bd7b08fd13a66d9d76ed43aac6e6c43eb6832e6d782e294d1061be5\",\"contractAddress\":\"TKcEU8ekq2ZoFzLSGFYCUY6aocJBX9X31b\",\"callerAddress\":\"\",\"originAddress\":\"TActor4gUERiLmWB9AwseX4tkQPzfoposU\",\"creatorAddress\":\"TAFotzexiiUJzGkBHDy9Jbn7rVHoYyWuLA\",\"blockNumber\":46789336,\"blockHash\":\"0000000002c9f2d8f6afb940898cd89d1d9041ca6a2a069f46fd5b79422e0714\",\"removed\":false,\"latestSolidifiedBlockNumber\":46789317,\"rawData\":{\"address\":\"69b9bf3ee6a4781dcac4915a2d4878f9301d63a6\",\"topics\":[\"8b3e96f2b889fa771c53c981b40daf005f63f637f1869f707052d15a3dd97140\",\"000000000000000000000000459f749a62c6afbcd62b8bae33b9d50e433da0e1\"],\"data\":\"0000000000000000000000000000000000000000000000000000000000000002000000000000000000000000000000000000000000000000000000004403e96b000000000000000000000000000000000000000000000000000000000000000100000000000000000000000000000000000000000000003dd11840a1f1a5c425\"},\"eventSignature\":\"TokenExchange(address,int128,uint256,int128,uint256)\",\"eventSignatureFull\":\"TokenExchange(address buyer,int128 sold_id,uint256 tokens_sold,int128 bought_id,uint256 tokens_bought)\",\"eventName\":\"TokenExchange\",\"topicMap\":{\"0\":\"TGKLeTRMJyzpHq9ypyukxc3ee4DAyyfbqB\",\"buyer\":\"TGKLeTRMJyzpHq9ypyukxc3ee4DAyyfbqB\"},\"dataMap\":{\"sold_id\":\"2\",\"1\":\"2\",\"2\":\"1141107051\",\"tokens_bought\":\"1140318252113938727973\",\"3\":\"1\",\"4\":\"1140318252113938727973\",\"tokens_sold\":\"1141107051\",\"bought_id\":\"1\"}}";
        ContractEventLog eventLog = LogDecode.decodeContractEventLog(jsonString);
        String gsonString = GsonUtil.objectToGson(eventLog);
        Assert.equals(jsonString, gsonString);
    }
}
