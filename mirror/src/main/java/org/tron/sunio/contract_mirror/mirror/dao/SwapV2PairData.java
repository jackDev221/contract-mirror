package org.tron.sunio.contract_mirror.mirror.dao;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.math.BigInteger;

@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
public class SwapV2PairData extends BaseContractData {
    private String factory;
    private String token0;
    private String token1;
    private BigInteger reverse0;
    private BigInteger reverse1;
    private long blockTimestampLast;
    private int price0CumulativeLast;
    private int price1CumulativeLast;
    private int unlocked;
}
