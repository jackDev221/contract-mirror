package org.tron.sunio.contract_mirror.mirror.dao;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.tron.sunio.contract_mirror.event_decode.utils.GsonUtil;

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
        String jsonString = GsonUtil.objectToGson(this);
        return GsonUtil.gsonToObject(jsonString, SwapV1Data.class);
    }
}
