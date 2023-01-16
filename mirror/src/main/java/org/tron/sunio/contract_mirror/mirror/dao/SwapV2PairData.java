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
    private String name;
    private String symbol;
    private long decimals;
    private String factory;
    private String token0;
    private String token1;
    private BigInteger reverse0;
    private BigInteger reverse1;
    private BigInteger kLast;
    private long blockTimestampLast;
    private long price0CumulativeLast;
    private long price1CumulativeLast;
    private long unlocked;
    private BigInteger trxBalance;
    private BigInteger lpTotalSupply;
}
