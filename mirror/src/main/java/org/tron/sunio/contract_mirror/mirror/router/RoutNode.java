package org.tron.sunio.contract_mirror.mirror.router;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
public class RoutNode {
    String address;
    String symbol;
    String contract;
    String poolType;
    List<RoutNode> subNodes;


    public RoutNode(String address, String symbol, String contract, String poolType) {
        this.address = address;
        this.symbol = symbol;
        this.contract = contract;
        this.poolType = poolType;
        subNodes = new ArrayList<>();

    }
}
