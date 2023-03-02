package org.tron.defi.contract_mirror.core.token;

import java.math.BigInteger;

public interface IToken {
    int getDecimals();

    String getSymbol();

    void transfer(String issuer, String to, BigInteger amount);
}
