package org.tron.defi.contract_mirror.core.pool;


import lombok.extern.slf4j.Slf4j;
import org.tron.defi.contract.abi.ContractAbi;
import org.tron.defi.contract.abi.pool.SunswapV1Abi;
import org.tron.defi.contract_mirror.core.Contract;
import org.tron.defi.contract_mirror.core.token.ITRC20;
import org.tron.defi.contract_mirror.core.token.IToken;
import org.tron.defi.contract_mirror.core.token.TRC20;
import org.tron.defi.contract_mirror.core.token.TRX;
import org.tron.defi.contract_mirror.utils.chain.TronContractTrigger;
import org.web3j.abi.EventValues;
import org.web3j.abi.datatypes.generated.Uint256;

import java.math.BigInteger;

@Slf4j
public class SunswapV1Pool extends Pool implements IToken, ITRC20 {
    public SunswapV1Pool(String address) {
        super(address);
        lpToken = new TRC20(address);
        this.type = PoolType.SUNSWAP_V1;
    }

    @Override
    public BigInteger balanceOf(String address) {
        return getLpToken().balanceOf(address);
    }

    @Override
    public BigInteger balanceOfFromChain(String address) {
        return getLpToken().balanceOfFromChain(address);
    }

    @Override
    public BigInteger getTotalSupplyFromChain() {
        return getLpToken().getTotalSupplyFromChain();
    }

    @Override
    public void setBalance(String address, BigInteger balance) {
        getLpToken().setBalance(address, balance);
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
    public void init() {
        sync();
    }

    @Override
    protected void getContractData() {
        wlock.lock();
        try {
            TRX trx = (TRX) getTokens().get(0);
            ITRC20 token = (ITRC20) getTokens().get(1);
            trx.setBalance(getAddress(), trx.balanceOfFromChain(getAddress()));
            token.setBalance(getAddress(), token.balanceOfFromChain(getAddress()));
        } catch (Exception e) {
            log.error(e.getMessage());
        } finally {
            wlock.unlock();
        }
    }

    @Override
    protected void handleEvent(String eventName, EventValues eventValues, long eventTime) {
        if (eventName.equals("Snapshot")) {
            handleSnapshotEvent(eventValues);
        } else {
            log.warn("Ignore event " + eventName);
        }
    }

    @Override
    protected ContractAbi loadAbi() {
        return tronContractTrigger.contractAt(SunswapV1Abi.class, getAddress());
    }

    @Override
    public void setTronContractTrigger(TronContractTrigger tronContractTrigger) {
        super.setTronContractTrigger(tronContractTrigger);
        ((Contract) getLpToken()).setTronContractTrigger(tronContractTrigger);
    }

    private void handleSnapshotEvent(EventValues eventValues) {
        BigInteger trxBalance = ((Uint256) eventValues.getIndexedValues().get(1)).getValue();
        BigInteger tokenBalance = ((Uint256) eventValues.getNonIndexedValues().get(2)).getValue();
        wlock.lock();
        try {
            ((TRX) getTokens().get(0)).setBalance(getAddress(), trxBalance);
            ((ITRC20) getTokens().get(1)).setBalance(getAddress(), tokenBalance);
        } finally {
            wlock.unlock();
        }
    }
}
