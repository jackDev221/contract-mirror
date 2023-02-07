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
public class Curve4PoolData extends BaseContractData {
    private String[] coins = new String[2];
    private BigInteger[] balances = new BigInteger[2];
    private String[] baseCoins = new String[3];
    private String lpToken;
    private String basePool;
    private BigInteger baseVirtualPrice;
    private long baseCacheUpdated;
    private String baseLp;
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

    public void updateCoins(int index, String address) {
        if (index >= coins.length) {
            System.out.println("Out of range!!");
        }
        coins[index] = address;
    }

    public void updateBaseCoins(int index, String address) {
        if (index >= baseCoins.length) {
            System.out.println("Out of range!!");
        }
        baseCoins[index] = address;
    }

    public void updateBalances(int index, BigInteger value) {
        if (index >= balances.length) {
            System.out.println("Out of range!!");
        }
        balances[index] = value;
    }
}
