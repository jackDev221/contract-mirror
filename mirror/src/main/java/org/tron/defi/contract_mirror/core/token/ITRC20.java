package org.tron.defi.contract_mirror.core.token;

import java.math.BigInteger;

public interface ITRC20 {
    BigInteger balanceOf(String address);
}
