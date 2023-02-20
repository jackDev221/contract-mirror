package org.tron.sunio.contract_mirror.mirror.router;

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
    private int impact;
    private long inUsd;
    private long outUsd;
    private BigInteger amount;
    private double fee;
}
