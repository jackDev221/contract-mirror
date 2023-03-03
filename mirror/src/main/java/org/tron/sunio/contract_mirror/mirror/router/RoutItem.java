package org.tron.sunio.contract_mirror.mirror.router;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;


@Data
@NoArgsConstructor
public class RoutItem {
    private List<String> roadForName = new ArrayList<>();
    private List<String> roadForAddr = new ArrayList<>();
    private List<String> pool = new ArrayList<>();
    private String impact = "0";
    private String inUsd;
    private String outUsd;
    @JsonIgnore
    private BigInteger amountV;
    private String amount;
    private String fee;

    public static RoutItem getNullInstance() {
        // 对齐旧版的返回
        RoutItem routItem = new RoutItem();
        routItem.roadForAddr = null;
        routItem.roadForName = null;
        routItem.pool = null;
        routItem.inUsd = "0.000000";
        return routItem;
    }

}
