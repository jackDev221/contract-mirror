package org.tron.sunio.contract_mirror.mirror.dao;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.util.HashMap;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
public class SwapFactoryV2Data extends BaseContractData {
    private String feeTo;
    private String feeToSetter;
    private long pairCount;
    @JsonIgnore
    private Map<String, Map<String, String>> tokensToPairMaps = new HashMap<>();
    @JsonIgnore
    private Map<Integer, String> pairsMap = new HashMap<>();
}
