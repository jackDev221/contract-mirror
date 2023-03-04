package org.tron.defi.contract_mirror.core.pool;

import lombok.extern.slf4j.Slf4j;
import org.tron.defi.contract.abi.ContractAbi;
import org.tron.defi.contract.abi.EventPrototype;
import org.tron.defi.contract.abi.pool.SunswapV2Abi;
import org.tron.defi.contract.log.ContractLog;
import org.tron.defi.contract_mirror.core.Contract;
import org.tron.defi.contract_mirror.core.token.ITRC20;
import org.tron.defi.contract_mirror.core.token.IToken;
import org.tron.defi.contract_mirror.core.token.TRC20;
import org.tron.defi.contract_mirror.utils.chain.AddressConverter;
import org.tron.defi.contract_mirror.utils.chain.TronContractTrigger;
import org.web3j.abi.EventValues;
import org.web3j.abi.datatypes.Address;
import org.web3j.abi.datatypes.Type;
import org.web3j.abi.datatypes.generated.Uint112;

import java.math.BigInteger;
import java.util.Collections;
import java.util.List;

@Slf4j
public class SunswapV2Pool extends Pool implements IToken, ITRC20 {
    public SunswapV2Pool(String address) {
        super(address);
        type = PoolType.SUNSWAP_V2;
        lpToken = new TRC20(address);
    }

    public SunswapV2Pool(ITRC20 lpToken) {
        super(((Contract) lpToken).getAddress());
        type = PoolType.SUNSWAP_V2;
        this.lpToken = lpToken;
    }

    @Override
    public BigInteger balanceOf(String address) {
        return ((IToken) getLpToken()).balanceOf(address);
    }

    @Override
    public BigInteger balanceOfFromChain(String address) {
        return ((IToken) getLpToken()).balanceOfFromChain(address);
    }

    @Override
    public BigInteger getTotalSupplyFromChain() {
        return getLpToken().getTotalSupplyFromChain();
    }

    @Override
    public void setBalance(String address, BigInteger balance) {
        ((IToken) getLpToken()).setBalance(address, balance);
    }

    @Override
    public void setTotalSupply(BigInteger totalSupply) {
        getLpToken().setTotalSupply(totalSupply);
    }

    @Override
    public BigInteger totalSupply() {
        return getLpToken().totalSupply();
    }

    @Override
    public void transferFrom(String issuer, String from, BigInteger amount) {
        getLpToken().transferFrom(issuer, from, amount);
    }

    @Override
    public int getDecimals() {
        return ((IToken) getLpToken()).getDecimals();
    }

    @Override
    public String getSymbol() {
        return ((IToken) getLpToken()).getSymbol();
    }

    @Override
    public void transfer(String issuer, String to, BigInteger amount) {
        ((IToken) getLpToken()).transfer(issuer, to, amount);
    }

    @Override
    protected void doInitialize() {
        sync();
    }

    @Override
    protected void getContractData() {
        wlock.lock();
        try {
            IToken token0 = (IToken) getTokens().get(0);
            IToken token1 = (IToken) getTokens().get(1);
            token0.setBalance(getAddress(), token0.balanceOfFromChain(getAddress()));
            token1.setBalance(getAddress(), token1.balanceOfFromChain(getAddress()));
            log.info("{} balance {}", token0.getSymbol(), token0.balanceOf(getAddress()));
            log.info("{} balance {}", token1.getSymbol(), token1.balanceOf(getAddress()));
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            wlock.unlock();
        }
    }

    @Override
    protected void handleEvent(String eventName, EventValues eventValues, long eventTime) {
        switch (eventName) {
            case "Sync":
                handleSyncEvent(eventValues);
                break;
            default:
                log.warn("Ignore event {}", eventName);
                break;
        }
    }

    @Override
    protected ContractAbi loadAbi() {
        return tronContractTrigger.contractAt(SunswapV2Abi.class, getAddress());
    }

    @Override
    public EventValues decodeEvent(ContractLog log) {
        EventValues eventValues = abi.decodeEvent(log);
        if (null != eventValues) {
            return eventValues;
        }
        return ((Contract) getLpToken()).decodeEvent(log);
    }

    @Override
    public EventPrototype getEvent(String signature) {
        EventPrototype prototype = abi.getEvent(signature);
        if (null != prototype) {
            return prototype;
        }
        return ((Contract) getLpToken()).getEvent(signature);
    }

    @Override
    public void setTronContractTrigger(TronContractTrigger tronContractTrigger) {
        super.setTronContractTrigger(tronContractTrigger);
        ((Contract) getLpToken()).setTronContractTrigger(tronContractTrigger);
    }

    public ITRC20 getToken0() {
        if (tokens.size() > 0) {
            return (ITRC20) tokens.get(0);
        }
        return getTokenFromChain(0);
    }

    public ITRC20 getToken1() {
        if (tokens.size() > 1) {
            return (ITRC20) tokens.get(1);
        }
        return getTokenFromChain(1);
    }

    private ITRC20 getTokenFromChain(int n) {
        if (n > 1) {
            throw new IllegalArgumentException();
        }
        List<Type> response = abi.invoke((n == 0
                                          ? SunswapV2Abi.Functions.TOKEN0
                                          : SunswapV2Abi.Functions.TOKEN1),
                                         Collections.emptyList());
        String tokenAddress
            = AddressConverter.EthToTronBase58Address(((Address) response.get(0)).getValue());
        Contract contract = contractManager.getContract(tokenAddress);
        return contract != null
               ? (ITRC20) contract
               : (ITRC20) contractManager.registerContract(new TRC20(tokenAddress));
    }

    private void handleSyncEvent(EventValues eventValues) {
        BigInteger balance0 = ((Uint112) eventValues.getNonIndexedValues().get(0)).getValue();
        BigInteger balance1 = ((Uint112) eventValues.getNonIndexedValues().get(1)).getValue();
        wlock.lock();
        try {
            IToken token0 = (IToken) getTokens().get(0);
            IToken token1 = (IToken) getTokens().get(1);
            BigInteger balanceBefore0 = token0.balanceOf(getAddress());
            BigInteger balanceBefore1 = token1.balanceOf(getAddress());
            token0.setBalance(getAddress(), balance0);
            token1.setBalance(getAddress(), balance1);
            log.info("{} balance {} -> {}", token0.getSymbol(), balanceBefore0, balance0);
            log.info("{} balance {} -> {}", token1.getSymbol(), balanceBefore1, balance1);
        } finally {
            wlock.unlock();
        }
    }
}
