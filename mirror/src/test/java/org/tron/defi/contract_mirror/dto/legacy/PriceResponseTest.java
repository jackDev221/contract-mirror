package org.tron.defi.contract_mirror.dto.legacy;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.serializer.SerializerFeature;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

@Slf4j
public class PriceResponseTest {
    @Test
    public void jsonParseTest() {
        String json = "{\"data\":{\"USDD\":{\"quote\":{\"USD\":{\"last_updated\":1678607335339," +
                      "\"price\":\"1.000000000000000000\"}}}},\"status\":{\"error_code\":0," +
                      "\"error_message\":null}}";
        PriceResponse response = JSONObject.parseObject(json, PriceResponse.class);
        Assertions.assertEquals(json,
                                JSONObject.toJSONString(response,
                                                        SerializerFeature.WriteMapNullValue));
    }
}
