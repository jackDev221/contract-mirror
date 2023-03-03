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
public class CurveBasePoolData extends BaseContractData {
    private String[] coins;
    private String[] coinNames;
    private String[] coinSymbols;
    private BigInteger[] balances;
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
    private String poolName;

    public CurveBasePoolData(int count) {
        coins = new String[count];
        coinNames = new String[count];
        coinSymbols = new String[count];
        balances = new BigInteger[count];

    }

    public void updateCoins(int index, String address) {
        if (index >= coins.length) {
            System.out.println("Out of range!!");
        }
        coins[index] = address;
    }

    public void updateCoinNames(int index, String name) {
        if (index >= coinNames.length) {
            System.out.println("Out of range!!");
        }
        coinNames[index] = name;
    }

    public void updateCoinSymbols(int index, String symbol) {
        if (index >= coinSymbols.length) {
            System.out.println("Out of range!!");
        }
        coinSymbols[index] = symbol;
    }

    public void updateBalances(int index, BigInteger value) {
        if (index >= balances.length) {
            System.out.println("Out of range!!");
        }
        balances[index] = value;
    }

    public BigInteger[] getCopyBalances() {
        BigInteger[] result = new BigInteger[balances.length];
        for (int i = 0; i < balances.length; i++) {
            result[i] = new BigInteger(balances[i].toString());
        }
        return result;
    }

    public CurveBasePoolData copySelf() {
        return DeepCopyUtils.deepCopy(this, CurveBasePoolData.class);
    }

    public BigInteger[] copyBalances() {
        return DeepCopyUtils.deepCopy(this.balances, BigInteger[].class);
    }

    public int[] getTokensIndex(String token0, String token1) {
        int[] res = new int[]{-1, -1};
        for (int i = 0; i < coins.length; i++) {
            if (coins[i].equalsIgnoreCase(token0)) {
                res[0] = i;
            }
            if (coins[i].equalsIgnoreCase(token1)) {
                res[1] = i;
            }
        }
        return res;
    }
}
