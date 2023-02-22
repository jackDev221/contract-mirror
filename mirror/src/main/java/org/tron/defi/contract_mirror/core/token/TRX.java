package org.tron.defi.contract_mirror.core.token;

import org.tron.defi.contract.abi.ContractAbi;
import org.tron.defi.contract_mirror.common.ContractType;

import java.math.BigInteger;
import java.util.concurrent.ConcurrentHashMap;

public class TRX extends Token {
    private static final String TRX_ADDRESS = "T9yD14Nj9j7xAB4dbGeiX9h8unkKHxuWwb";
    private static final String TRX_SYMBOL = "TRX";
    private static final int TRX_DECIMALS = 6;
    private static final TRX instance = new TRX();
    private final ConcurrentHashMap<String, BigInteger> balances = new ConcurrentHashMap<>(30000);

    private TRX() {
        super(TRX_ADDRESS);
        symbol = TRX_SYMBOL;
        decimals = TRX_DECIMALS;
    }

    public static TRX getInstance() {
        return instance;
    }

    @Override
    protected ContractAbi loadAbi() {
        return null;
    }

    @Override
    public String getContractType() {
        return ContractType.TRX_TOKEN.name();
    }

    public BigInteger balanceOf(String address) {
        BigInteger balance = balances.getOrDefault(address, null);
        if (null != balance) {
            return balance;
        }
        return balanceOfFromChain(address);
    }

    public BigInteger balanceOfFromChain(String address) {
        return new BigInteger(String.valueOf(tronContractTrigger.balance(address)));
    }

    public void setBalance(String address, BigInteger balance) {
        balances.put(address, balance);
    }
}
