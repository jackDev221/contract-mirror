package org.tron.defi.contract_mirror.core.token;

import org.tron.defi.contract.abi.ContractAbi;
import org.tron.defi.contract_mirror.common.ContractType;
import org.tron.defi.contract_mirror.core.Contract;
import org.tron.defi.contract_mirror.utils.TokenMath;

import java.math.BigInteger;
import java.util.concurrent.ConcurrentHashMap;

public class TRX extends Contract implements IToken {
    private static final String TRX_ADDRESS = "T9yD14Nj9j7xAB4dbGeiX9h8unkKHxuWwb";
    private static final String TRX_SYMBOL = "TRX";
    private static final int TRX_DECIMALS = 6;
    private static final TRX instance = new TRX();
    private final ConcurrentHashMap<String, BigInteger> balances = new ConcurrentHashMap<>(30000);

    private TRX() {
        super(TRX_ADDRESS);
    }

    @Override
    public String getContractType() {
        return ContractType.TRX_TOKEN.name();
    }

    @Override
    protected ContractAbi loadAbi() {
        return null;
    }

    @Override
    public int getDecimals() {
        return TRX_DECIMALS;
    }

    @Override
    public String getSymbol() {
        return TRX_SYMBOL;
    }

    @Override
    public void transfer(String issuer, String to, BigInteger amount) {
        TokenMath.decreaseTRXBalance(issuer, amount);
        TokenMath.increaseTRXBalance(to, amount);
    }

    public static TRX getInstance() {
        return instance;
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
