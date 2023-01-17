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
public class Curve2PoolData extends BaseContractData {
    private String token;
    private BigInteger fee;
    private BigInteger futureFee;
    private BigInteger adminFee;
    private BigInteger futureAdminFee;
    private BigInteger adminActionsDeadline;
    private String feeConverter;
    private BigInteger initialA;
    private BigInteger initialATime;
    private BigInteger futureA;
    private BigInteger futureATime;
    private String owner;
    private String futureOwner;
    private BigInteger transferOwnershipDeadline;
}
