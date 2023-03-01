package org.tron.sunio.contract_mirror.mirror.router;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.math.BigInteger;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RouterInput {
    private String fromToken;
    private String toToken;
    private String fromTokenSymbol;
    private String toTokenSymbol;
    private int fromDecimal;
    private int toDecimal;
    private BigInteger in;
    private BigDecimal fromPrice;
    private BigDecimal toPrice;
    private boolean isUseBaseTokens;
}
