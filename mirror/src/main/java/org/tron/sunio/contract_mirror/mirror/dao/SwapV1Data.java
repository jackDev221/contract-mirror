package org.tron.sunio.contract_mirror.mirror.dao;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.springframework.beans.BeanUtils;

import java.math.BigInteger;

@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
public class SwapV1Data extends BaseContractData {
    private String tokenAddress;
    private String name;
    private String symbol;
    private long decimals;
    private BigInteger kLast;
    private BigInteger trxBalance;
    private BigInteger tokenBalance;
    private BigInteger lpTotalSupply;
    private String tokenName;
    private String tokenSymbol;

    public SwapV1Data copySelf() {
        SwapV1Data res = new SwapV1Data();
        BeanUtils.copyProperties(this, res);
        return res;
    }
}
