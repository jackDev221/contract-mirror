package org.tron.defi.contract_mirror.core.token;

import com.alibaba.fastjson.JSONObject;
import lombok.Setter;
import org.tron.defi.contract.abi.ContractAbi;
import org.tron.defi.contract.abi.token.TRC20Abi;
import org.tron.defi.contract_mirror.common.ContractType;
import org.tron.defi.contract_mirror.core.Contract;
import org.tron.defi.contract_mirror.utils.chain.AddressConverter;
import org.web3j.abi.datatypes.Type;
import org.web3j.abi.datatypes.Utf8String;
import org.web3j.abi.datatypes.generated.Uint256;

import java.math.BigInteger;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

public class TRC20 extends Contract implements IToken, ITRC20 {
    private final ConcurrentHashMap<String, BigInteger> balances = new ConcurrentHashMap<>(30000);
    private String symbol;
    private int decimals = 0;
    @Setter
    private BigInteger totalSupply;

    @Override
    public String getSymbol() {
        if (null != symbol) {
            return symbol;
        }
        return getSymbolFromChain();
    }

    @Override
    public void transfer(String issuer, String to, BigInteger amount) {
        BigInteger balance = balanceOf(issuer).subtract(amount);
        if (balance.compareTo(BigInteger.ZERO) < 0) {
            throw new IllegalStateException("NOT ENOUGH BALANCE " + issuer);
        }
        setBalance(issuer, balance);
        setBalance(to, balanceOf(to).add(amount));
    }

    public TRC20(String address) {
        super(address);
    }

    @Override
    public BigInteger balanceOf(String address) {
        BigInteger result = balances.getOrDefault(address, null);
        return null == result ? balanceOfFromChain(address) : result;
    }

    @Override
    public String run(String method) {
        if (0 == method.compareTo("symbol")) {
            return getSymbol();
        } else if (0 == method.compareTo("decimals")) {
            return String.valueOf(getDecimals());
        } else {
            return super.run(method);
        }
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
    public JSONObject getInfo() {
        JSONObject info = super.getInfo();
        info.put("symbol", getSymbol());
        info.put("decimals", getDecimals());
        return info;
    }

    public void setBalance(String address, BigInteger balance) {
        balances.put(address, balance);
    }

    @Override
    public int getDecimals() {
        if (0 != decimals) {
            return decimals;
        }
        return getDecimalsFromChain();
    }

    @Override
    public BigInteger totalSupply() {
        if (null != totalSupply) {
            return totalSupply;
        }
        return getTotalSupplyFromChain();
    }

    @Override
    public void transferFrom(String issuer, String from, BigInteger amount) {
        transfer(from, issuer, amount);
    }

    public BigInteger getTotalSupplyFromChain() {
        List<Type> response = abi.invoke(TRC20Abi.Functions.TOTAL_SUPPLY, Collections.emptyList());
        return ((Uint256) response.get(0)).getValue();
    }

    public BigInteger balanceOfFromChain(String address) {
        String ethAddress = AddressConverter.TronBase58ToEthAddress(address);
        System.out.println(ethAddress);
        List<Type> response = abi.invoke(TRC20Abi.Functions.BALANCE_OF,
                                         Collections.singletonList(ethAddress));
        return ((Uint256) response.get(0)).getValue();
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
