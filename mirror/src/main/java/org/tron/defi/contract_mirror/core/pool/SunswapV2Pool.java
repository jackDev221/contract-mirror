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
import org.web3j.abi.datatypes.generated.Uint32;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@Slf4j
public class SunswapV2Pool extends Pool implements IToken, ITRC20 {
    private static final BigInteger FEE_NUMERATOR = BigInteger.valueOf(997);
    private static final BigInteger FEE_DENOMINATOR = BigInteger.valueOf(1000);

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
        BigInteger balanceIn;
        BigInteger balanceOut;
        rlock.lock();
        try {
            balanceIn = fromToken.balanceOf(getAddress());
            balanceOut = toToken.balanceOf(getAddress());
        } finally {
            rlock.unlock();
        }
        BigInteger amountInWithFee = amountIn.multiply(FEE_NUMERATOR);
        BigInteger amountOut = amountInWithFee.multiply(balanceOut)
                                              .divide(balanceIn.multiply(FEE_DENOMINATOR)
                                                               .add(amountInWithFee));
        if (balanceOut.compareTo(amountOut) < 0) {
            throw new RuntimeException("NOT ENOUGH BALANCE");
        }
        return amountOut;
    }

    @Override
    public BigInteger getApproximateFee(IToken fromToken, IToken toToken, BigInteger amountIn) {
        return amountIn.multiply(FEE_DENOMINATOR.subtract(FEE_NUMERATOR)).divide(FEE_DENOMINATOR);
    }

    @Override
    public BigInteger getPrice(IToken fromToken, IToken toToken) {
        fromToken = getTokenByAddress(((Contract) fromToken).getAddress());
        toToken = getTokenByAddress(((Contract) toToken).getAddress());
        if (null == fromToken || null == toToken) {
            throw new IllegalArgumentException();
        }
        return BigInteger.valueOf(10)
                         .pow(PRICE_DECIMALS)
                         .multiply(fromToken.balanceOf(getAddress()))
                         .divide(toToken.balanceOf(getAddress()));
    }

    @Override
    protected void doInitialize() {
        sync();
    }

    @Override
    protected void getContractData() {
        List<BigInteger> reserves = getReservesFromChain();
        timestamp0 = reserves.get(2).longValue() * 1000;   // updateTime
        wlock.lock();
        try {
            IToken token0 = (IToken) getTokens().get(0);
            IToken token1 = (IToken) getTokens().get(1);
            token0.setBalance(getAddress(), reserves.get(0));
            token1.setBalance(getAddress(), reserves.get(1));
            log.info("{} balance {}", token0.getSymbol(), token0.balanceOf(getAddress()));
            log.info("{} balance {}", token1.getSymbol(), token1.balanceOf(getAddress()));
            log.info("updateTime {}", timestamp0);
        } finally {
            wlock.unlock();
        }
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
            case "Sync":
                return diffBalances();
            default:
                return false;
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

    public List<BigInteger> getReserves() {
        IToken token0 = (IToken) getToken0();
        IToken token1 = (IToken) getToken1();
        rlock.lock();
        try {
            return Arrays.asList(token0.balanceOf(getAddress()),
                                 token1.balanceOf(getAddress()),
                                 BigInteger.valueOf(lastEventTimestamp));
        } finally {
            rlock.unlock();
        }
    }

    public List<BigInteger> getReservesFromChain() {
        List<Type> response = abi.invoke(SunswapV2Abi.Functions.GET_RESERVERS,
                                         Collections.emptyList());
        return Arrays.asList(((Uint112) response.get(0)).getValue(),
                             ((Uint112) response.get(1)).getValue(),
                             ((Uint32) response.get(2)).getValue());
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

    private boolean diffBalances() {
        log.info("diffBalances {}", getAddress());
        IToken token0 = (IToken) getTokens().get(0);
        IToken token1 = (IToken) getTokens().get(1);
        List<BigInteger> reserves = getReservesFromChain();
        long updateTime = reserves.get(2).longValue() * 1000;
        if (updateTime != lastEventTimestamp) {
            log.warn("balances has been changed [{}, {}]", lastEventTimestamp, updateTime);
            return false;
        }
        BigInteger localBalance0;
        BigInteger localBalance1;
        rlock.lock();
        try {
            localBalance0 = token0.balanceOf(getAddress());
            localBalance1 = token1.balanceOf(getAddress());
        } finally {
            rlock.unlock();
        }
        if (0 != localBalance0.compareTo(reserves.get(0)) ||
            0 != localBalance1.compareTo(reserves.get(1))) {
            log.info("expect balance0 {}", reserves.get(0));
            log.info("expect balance1 {}", reserves.get(1));
            log.info("local balance0 {}", localBalance0);
            log.info("local balance1 {}", localBalance1);
            return true;
        }
        return false;
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
        log.info("handleSyncEvent {}", getAddress());
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
