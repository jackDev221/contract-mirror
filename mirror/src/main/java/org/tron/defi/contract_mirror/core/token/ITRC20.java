package org.tron.defi.contract_mirror.core.token;

import java.math.BigInteger;

public interface ITRC20 {
    BigInteger balanceOf(String address);

    BigInteger balanceOfFromChain(String address);

    BigInteger getTotalSupplyFromChain();

    void setBalance(String address, BigInteger balance);

    void setTotalSupply(BigInteger totalSupply);

    BigInteger totalSupply();

    void transferFrom(String issuer, String from, BigInteger amount);
}
