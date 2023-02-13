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
public class CurveBasePoolData extends BaseContractData {
    private String[] coins;
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

    public CurveBasePoolData(int count) {
        coins = new String[count];
        balances = new BigInteger[count];
    }

    public void updateCoins(int index, String address) {
        if (index >= coins.length) {
            System.out.println("Out of range!!");
        }
        coins[index] = address;
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
        CurveBasePoolData res = new CurveBasePoolData();
        BeanUtils.copyProperties(this, res);
        return res;
    }

    public BigInteger[] copyBalances() {
        BigInteger[] res = new BigInteger[this.balances.length];
        BeanUtils.copyProperties(balances, res);
        return res;
    }
}
