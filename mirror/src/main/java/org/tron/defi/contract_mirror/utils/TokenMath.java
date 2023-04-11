package org.tron.defi.contract_mirror.utils;

import org.tron.defi.contract_mirror.core.token.IToken;
import org.tron.defi.contract_mirror.core.token.TRX;

import java.math.BigInteger;

public class TokenMath {
    public static BigInteger decreaseBalance(IToken token, String address, BigInteger amount) {
        BigInteger oldBalance = token.balanceOf(address);
        if (amount.compareTo(BigInteger.ZERO) == 0) {
            return oldBalance;
        }
        BigInteger newBalance = safeSubtract(oldBalance, amount);
        token.setBalance(address, newBalance);
        return newBalance;
    }

    public static BigInteger decreaseTRXBalance(String address, BigInteger amount) {
        BigInteger oldBalance = TRX.getInstance().balanceOf(address);
        if (amount.compareTo(BigInteger.ZERO) == 0) {
            return oldBalance;
        }
        BigInteger newBalance = safeSubtract(oldBalance, amount);
        TRX.getInstance().setBalance(address, newBalance);
        return newBalance;
    }

    public static BigInteger increaseBalance(IToken token, String address, BigInteger amount) {
        BigInteger oldBalance = token.balanceOf(address);
        if (amount.compareTo(BigInteger.ZERO) == 0) {
            return oldBalance;
        }
        BigInteger newBalance = safeAdd(oldBalance, amount);
        token.setBalance(address, newBalance);
        return newBalance;
    }

    public static BigInteger increaseTRXBalance(String address, BigInteger amount) {
        BigInteger oldBalance = TRX.getInstance().balanceOf(address);
        if (amount.compareTo(BigInteger.ZERO) == 0) {
            return oldBalance;
        }
        BigInteger newBalance = safeAdd(oldBalance, amount);
        TRX.getInstance().setBalance(address, newBalance);
        return newBalance;
    }

    public static BigInteger safeAdd(BigInteger oldBalance, BigInteger amount) {
        BigInteger newBalance = oldBalance.add(amount);
        if (newBalance.compareTo(oldBalance) < 0) {
            throw new RuntimeException("OVERFLOW");
        }
        return newBalance;
    }

    public static BigInteger safeSubtract(BigInteger oldBalance, BigInteger amount) {
        BigInteger newBalance = oldBalance.subtract(amount);
        if (newBalance.compareTo(oldBalance) > 0) {
            throw new RuntimeException("OVERFLOW");
        }
        return newBalance;
    }
}
