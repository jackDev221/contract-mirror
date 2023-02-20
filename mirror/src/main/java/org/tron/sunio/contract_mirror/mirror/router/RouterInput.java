package org.tron.sunio.contract_mirror.mirror.router;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigInteger;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RouterInput {
    private String fromToken;
    private String toToken;
    private String fromName;
    private String toName;
    private int fromDecimal;
    private int toDecimal;
    private BigInteger in;
}
