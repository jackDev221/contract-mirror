package org.tron.sunio.contract_mirror.mirror.dao;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.math.BigInteger;

@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper=false)
public class ContractV1Data extends BaseContractData{
    private String tokenAddress;
    private String name;
    private String symbol;
    private long decimals;
    private int kLast;
    private BigInteger trxBalance;
}
