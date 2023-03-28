package org.tron.defi.contract_mirror.strategy;

import org.tron.defi.contract_mirror.dao.RouterInfo;

import java.math.BigInteger;

public interface IStrategy {
    RouterInfo getPath(String from, String to, BigInteger amountIn);
}
