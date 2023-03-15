package org.tron.defi.contract_mirror.core.pool;

import lombok.extern.slf4j.Slf4j;
import org.tron.defi.contract.abi.ContractAbi;
import org.tron.defi.contract.abi.EventPrototype;
import org.tron.defi.contract.abi.pool.WTRXAbi;
import org.tron.defi.contract.log.ContractLog;
import org.tron.defi.contract_mirror.core.Contract;
import org.tron.defi.contract_mirror.core.token.ITRC20;
import org.tron.defi.contract_mirror.core.token.IToken;
import org.tron.defi.contract_mirror.core.token.TRC20;
import org.tron.defi.contract_mirror.core.token.TRX;
import org.tron.defi.contract_mirror.utils.TokenMath;
import org.tron.defi.contract_mirror.utils.chain.AddressConverter;
import org.tron.defi.contract_mirror.utils.chain.TronContractTrigger;
import org.web3j.abi.EventValues;
import org.web3j.abi.datatypes.Address;
import org.web3j.abi.datatypes.generated.Uint256;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;

@Slf4j
public class WTRX extends Pool implements IToken, ITRC20 {
    public WTRX(String address) {
        super(address);
        type = PoolType.WTRX;
        lpToken = new TRC20(address);
        setTokens(new ArrayList<>(Arrays.asList(TRX.getInstance(), (Contract) lpToken)));
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
        if (TRX.getInstance() == toToken &&
            toToken.balanceOf(getAddress()).compareTo(amountTo) < 0) {
            throw new RuntimeException(getName() + " NOT ENOUGH BALANCE");
        }
        return amountTo;
    }

    @Override
    public BigInteger getApproximateFee(IToken fromToken, IToken toToken, BigInteger amountIn) {
        return BigInteger.ZERO;
    }

    @Override
    public BigInteger getPrice(IToken fromToken, IToken toToken) {
        return PRICE_FACTOR;
    }

    @Override
    protected void doInitialize() {
        sync();
    }

    @Override
    protected void getContractData() {
        wlock.lock();
        try {
            TRX trx = TRX.getInstance();
            trx.setBalance(getAddress(), trx.balanceOfFromChain(getAddress()));
            log.info("{} balance {}", trx.getSymbol(), trx.balanceOf(getAddress()));

            setTotalSupply(getTotalSupplyFromChain());
            log.info("totalSupply = {}", getLpToken().totalSupply());
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
            case "Deposit":
            case "Withdrawal":
                log.info("diff {} {}", eventName, getAddress());
                return diffBalances();
            default:
                return false;
        }
    }

    @Override
    protected void handleEvent(String eventName, EventValues eventValues, long eventTime) {
        switch (eventName) {
            case "Deposit":
                handleDepositEvent(eventValues);
                return;
            case "Withdrawal":
                handleWithdrawEvent(eventValues);
                return;
            default:
                log.warn("Ignore {} event", eventName);
                break;
        }
    }

    @Override
    protected ContractAbi loadAbi() {
        return tronContractTrigger.contractAt(WTRXAbi.class, getAddress());
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

    private boolean diffBalances() {
        TRX trx = TRX.getInstance();
        BigInteger expectTrxBalance = trx.balanceOfFromChain(getAddress());
        BigInteger expectTotalSupply = getTotalSupplyFromChain();
        BigInteger localTrxBalance;
        BigInteger localTotalSupply;
        rlock.lock();
        try {
            localTrxBalance = trx.balanceOf(getAddress());
            localTotalSupply = totalSupply();
        } finally {
            rlock.unlock();
        }
        if (0 != localTrxBalance.compareTo(expectTrxBalance) ||
            0 != localTotalSupply.compareTo(expectTotalSupply)) {
            log.info("expect trxBalance {}", expectTrxBalance);
            log.info("expect totalSupply {}", expectTotalSupply);
            log.info("local trxBalance {}", localTrxBalance);
            log.info("local totalSupply {}", localTotalSupply);
            return true;
        }
        return false;
    }

    private void handleDepositEvent(EventValues eventValues) {
        log.info("handleDepositEvent {}", getAddress());
        String provider
            = AddressConverter.EthToTronBase58Address(((Address) eventValues.getIndexedValues()
                                                                            .get(0)).getValue());
        BigInteger trxAmount = ((Uint256) eventValues.getNonIndexedValues().get(0)).getValue();

        BigInteger balanceBefore = TRX.getInstance().balanceOf(getAddress());
        BigInteger balanceAfter = TokenMath.increaseTRXBalance(getAddress(), trxAmount);
        log.info("trxBalance {} -> {}", balanceBefore, balanceAfter);

        TokenMath.increaseBalance((IToken) getLpToken(), provider, trxAmount);
        balanceBefore = totalSupply();
        balanceAfter = TokenMath.safeAdd(balanceBefore, trxAmount);
        setTotalSupply(balanceAfter);
        log.info("totalSupply {} -> {}", balanceBefore, balanceAfter);
    }

    private void handleWithdrawEvent(EventValues eventValues) {
        log.info("handleWithdrawEvent {}", getAddress());
        String provider
            = AddressConverter.EthToTronBase58Address(((Address) eventValues.getIndexedValues()
                                                                            .get(0)).getValue());
        BigInteger trxAmount = ((Uint256) eventValues.getNonIndexedValues().get(0)).getValue();
        BigInteger balanceBefore = TRX.getInstance().balanceOf(getAddress());
        BigInteger balanceAfter = TokenMath.decreaseTRXBalance(getAddress(), trxAmount);
        log.info("trxBalance {} -> {}", balanceBefore, balanceAfter);

        TokenMath.decreaseBalance((IToken) getLpToken(), provider, trxAmount);
        balanceBefore = totalSupply();
        balanceAfter = TokenMath.safeSubtract(totalSupply(), trxAmount);
        setTotalSupply(balanceAfter);
        log.info("totalSupply {} -> {}", balanceBefore, balanceAfter);
    }
}
