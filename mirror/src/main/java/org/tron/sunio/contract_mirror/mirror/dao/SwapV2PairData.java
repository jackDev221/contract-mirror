package org.tron.sunio.contract_mirror.mirror.dao;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.tron.sunio.contract_mirror.mirror.tools.DeepCopyUtils;

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
    private long token0Decimals;
    private long token1Decimals;
    private BigInteger reserve0;
    private BigInteger reserve1;
    private BigInteger kLast;
    private long blockTimestampLast;
    private BigInteger price0CumulativeLast;
    private BigInteger price1CumulativeLast;
    private long unlocked;
    private BigInteger trxBalance;
    private BigInteger lpTotalSupply;
    private String token0Name;
    private String token0Symbol;
    private String token1Name;
    private String token1Symbol;


    public Reserves getReserves() {
        return new Reserves(reserve0, reserve1, blockTimestampLast);
    }

    @Data
    public static class Reserves {
        private BigInteger reserve0;
        private BigInteger reserve1;
        private long blockTimestampLast;

        public Reserves(BigInteger reserve0, BigInteger reserve1, long blockTimestampLast) {
            this.reserve0 = reserve0;
            this.reserve1 = reserve1;
            this.blockTimestampLast = blockTimestampLast;
        }
    }

    public SwapV2PairData copySelf() {
        return DeepCopyUtils.deepCopy(this, SwapV2PairData.class);
    }
}
