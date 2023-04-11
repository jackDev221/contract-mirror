package org.tron.defi.contract_mirror.core.token;

import java.math.BigInteger;

public interface IToken {
    BigInteger balanceOf(String address);

    BigInteger balanceOfFromChain(String address);

    int getDecimals();

    String getSymbol();

    void setBalance(String address, BigInteger balance);

    void transfer(String issuer, String to, BigInteger amount);
}
