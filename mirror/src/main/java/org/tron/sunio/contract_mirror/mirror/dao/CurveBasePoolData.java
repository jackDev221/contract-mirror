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
public class CurveBasePoolData extends BaseContractData {
    private String[] coins;
    private BigInteger[] balance;
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
    private BigInteger totalSupply;

    public CurveBasePoolData(int count) {
        coins = new String[count];
        balance = new BigInteger[count];
    }

    public void updateCoins(int index, String address) {
        if (index >= coins.length) {
            System.out.println("Out of range!!");
        }
        coins[index] = address;
    }

    public void updateBalances(int index, BigInteger value) {
        if (index >= balance.length) {
            System.out.println("Out of range!!");
        }
        balance[index] = value;
    }
}
