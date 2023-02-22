package org.tron.defi.contract_mirror.core.token;

import org.tron.defi.contract.abi.ContractAbi;
import org.tron.defi.contract.abi.token.TRC20Abi;
import org.tron.defi.contract_mirror.common.ContractType;
import org.web3j.abi.datatypes.Type;
import org.web3j.abi.datatypes.Utf8String;
import org.web3j.abi.datatypes.generated.Uint256;

import java.math.BigInteger;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

public class TRC20 extends Token implements ITRC20 {
    private final ConcurrentHashMap<String, BigInteger> balances = new ConcurrentHashMap<>(30000);

    public TRC20(String address) {
        super(address);
    }

    @Override
    public BigInteger balanceOf(String address) {
        BigInteger result = balances.getOrDefault(address, null);
        return null == result ? balanceOfFromChain(address) : result;
    }

    @Override
    public String getContractType() {
        return ContractType.TRC20_TOKEN.name();
    }

    @Override
    protected ContractAbi loadAbi() {
        return tronContractTrigger.contractAt(TRC20Abi.class, getAddress());
    }

    @Override
    public String getSymbol() {
        if (null != symbol) {
            return symbol;
        }
        return getSymbolFromChain();
    }

    @Override
    public int getDecimals() {
        if (0 != decimals) {
            return decimals;
        }
        return getDecimalsFromChain();
    }

    public BigInteger balanceOfFromChain(String address) {
        List<Type> response = abi.invoke(TRC20Abi.Functions.BALANCE_OF,
                                         Collections.singletonList(address));
        return ((Uint256) response.get(0)).getValue();
    }

    public void setBalance(String address, BigInteger balance) {
        balances.put(address, balance);
    }

    private int getDecimalsFromChain() {
        List<Type> response = abi.invoke(TRC20Abi.Functions.DECIMALS, Collections.emptyList());
        decimals = ((Uint256) response.get(0)).getValue().intValue();
        return decimals;
    }

    private String getSymbolFromChain() {
        List<Type> response = abi.invoke(TRC20Abi.Functions.SYMBOL, Collections.emptyList());
        symbol = ((Utf8String) response.get(0)).getValue();
        return symbol;
    }
}
