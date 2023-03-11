package org.tron.defi.contract_mirror.core.pool;

import lombok.extern.slf4j.Slf4j;
import org.tron.defi.contract.abi.ContractAbi;
import org.tron.defi.contract.abi.pool.WTRXAbi;
import org.tron.defi.contract_mirror.core.Contract;
import org.tron.defi.contract_mirror.core.token.ITRC20;
import org.tron.defi.contract_mirror.core.token.IToken;
import org.tron.defi.contract_mirror.core.token.TRC20;
import org.tron.defi.contract_mirror.core.token.TRX;
import org.tron.defi.contract_mirror.utils.chain.TronContractTrigger;
import org.web3j.abi.EventValues;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;

@Slf4j
public class WTRX extends Pool implements IToken, ITRC20 {
    public WTRX(String address) {
        super(address);
        type = PoolType.WTRX;
        lpToken = new TRC20(address);
        setTokens(new ArrayList<>(Arrays.asList((Contract) lpToken, TRX.getInstance())));
    }

    public WTRX(ITRC20 lpToken) {
        super(((Contract) lpToken).getAddress());
        type = PoolType.WTRX;
        this.lpToken = lpToken;
        setTokens(new ArrayList<>(Arrays.asList(TRX.getInstance(), (Contract) lpToken)));
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
    public int getDecimals() {
        return ((IToken) getLpToken()).getDecimals();
    }

    @Override
    public String getSymbol() {
        return ((IToken) getLpToken()).getSymbol();
    }

    @Override
    public void setBalance(String address, BigInteger balance) {
        ((IToken) getLpToken()).setBalance(address, balance);
    }

    @Override
    public void transfer(String issuer, String to, BigInteger amount) {
        ((IToken) getLpToken()).transfer(issuer, to, amount);
    }

    @Override
    public BigInteger getAmountOutUnsafe(IToken fromToken, IToken toToken, BigInteger amountIn) {
        BigInteger amountTo = amountIn;
        if (toToken.balanceOf(getAddress()).compareTo(amountTo) < 0) {
            throw new RuntimeException("NOT ENOUGH BALANCE");
        }
        return amountTo;
    }

    @Override
    public BigInteger getApproximateFee(IToken fromToken, IToken toToken, BigInteger amountIn) {
        return BigInteger.ZERO;
    }

    @Override
    public BigInteger getPrice(IToken fromToken, IToken toToken) {
        return BigInteger.valueOf(10).pow(PRICE_DECIMALS);
    }

    @Override
    protected void doInitialize() {
        sync();
    }

    @Override
    protected void getContractData() {
        wlock.lock();
        try {
            IToken wtrx = (IToken) getTokens().get(0);
            IToken trx = (IToken) getTokens().get(1);
            trx.setBalance(getAddress(), trx.balanceOfFromChain(getAddress()));
            wtrx.setBalance(getAddress(), wtrx.balanceOfFromChain(getAddress()));
            log.info("{} balance {}", trx.getSymbol(), trx.balanceOf(getAddress()));
            log.info("{} balance {}", wtrx.getSymbol(), wtrx.balanceOf(getAddress()));
        } finally {
            wlock.unlock();
        }
    }

    @Override
    public int cost() {
        return 0;
    }

    @Override
    protected void updateName() {
        name = type.name();
    }

    @Override
    public BigInteger getTotalSupplyFromChain() {
        return getLpToken().getTotalSupplyFromChain();
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
    protected boolean doDiff(String eventName) {
        switch (eventName) {
            default:
                return false;
        }
    }

    @Override
    protected void handleEvent(String eventName, EventValues eventValues, long eventTime) {
        switch (eventName) {
            default:
                log.warn("Ignore {} event");
        }
    }

    @Override
    protected ContractAbi loadAbi() {
        return tronContractTrigger.contractAt(WTRXAbi.class, getAddress());
    }

    @Override
    public void setTronContractTrigger(TronContractTrigger tronContractTrigger) {
        super.setTronContractTrigger(tronContractTrigger);
        ((Contract) getLpToken()).setTronContractTrigger(tronContractTrigger);
    }
}
