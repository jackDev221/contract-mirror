package org.tron.sunio.contract_mirror.mirror.dao;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.tron.sunio.contract_mirror.mirror.tools.DeepCopyUtils;

import java.util.HashMap;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
public class SwapFactoryV1Data extends BaseContractData {
    private String feeAddress;
    private long feeToRate;
    private long tokenCount;
    @JsonIgnore
    private Map<Integer, String> idTokenMap = new HashMap<>();
    @JsonIgnore
    private Map<String, String> exchangeToTokenMap = new HashMap<>();
    @JsonIgnore
    private Map<String, String> tokenToExchangeMap = new HashMap<>();

    public SwapFactoryV1Data copySelf() {
        return DeepCopyUtils.deepCopy(this, SwapFactoryV1Data.class);
    }
}
