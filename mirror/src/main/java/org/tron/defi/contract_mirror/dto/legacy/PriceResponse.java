package org.tron.defi.contract_mirror.dto.legacy;

import com.alibaba.fastjson.annotation.JSONField;
import lombok.Data;

import java.util.Map;

@Data
public class PriceResponse {
    private Map<String, Quote> data;
    private Status status;

    @Data
    public static class Status {
        @JSONField(name = "error_code")
        private int errorCode;
        @JSONField(name = "error_message")
        private String errorMessage;
    }

    @Data
    public static class Quote {
        @JSONField(name = "USD")
        private USD usd;
    }

    @Data
    public static class USD {
        @JSONField(name = "last_updated")
        public long lastUpdated;
        public String price;
    }
}
