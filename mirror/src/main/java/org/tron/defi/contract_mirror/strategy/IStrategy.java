package org.tron.defi.contract_mirror.strategy;

import org.tron.defi.contract_mirror.dao.RouterPath;

import java.math.BigInteger;
import java.util.List;

public interface IStrategy {
    List<RouterPath> getPath(String from, String to, BigInteger amountIn);
}
