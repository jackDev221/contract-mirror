package org.tron.defi.contract_mirror.core.pool;


import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.tron.defi.contract.abi.ContractAbi;
import org.tron.defi.contract.abi.EventPrototype;
import org.tron.defi.contract.abi.pool.SunswapV1Abi;
import org.tron.defi.contract.log.ContractLog;
import org.tron.defi.contract_mirror.core.Contract;
import org.tron.defi.contract_mirror.core.token.ITRC20;
import org.tron.defi.contract_mirror.core.token.IToken;
import org.tron.defi.contract_mirror.core.token.TRC20;
import org.tron.defi.contract_mirror.utils.chain.TronContractTrigger;
import org.web3j.abi.EventValues;
import org.web3j.abi.datatypes.generated.Uint256;

import java.math.BigInteger;

@Slf4j
public class SunswapV1Pool extends Pool implements IToken, ITRC20 {
    private static final BigInteger FEE_NUMERATOR = BigInteger.valueOf(997);
    private static final BigInteger FEE_DENOMINATOR = BigInteger.valueOf(1000);

    public SunswapV1Pool(String address) {
        super(address);
        type = PoolType.SUNSWAP_V1;
        lpToken = new TRC20(address);
    }

    public SunswapV1Pool(ITRC20 lpToken) {
        super(((Contract) lpToken).getAddress());
        type = PoolType.SUNSWAP_V1;
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
    public JSONObject getInfo() {
        JSONObject info = super.getInfo();
        JSONArray balances = new JSONArray();
        IToken token0 = (IToken) getTokens().get(0);
        balances.add(token0.balanceOf(getAddress()));
        IToken token1 = (IToken) getTokens().get(1);
        balances.add(token1.balanceOf(getAddress()));
        info.put("balances", balances);
        return info;
    }

    @Override
    public BigInteger getAmountOutUnsafe(IToken fromToken, IToken toToken, BigInteger amountIn) {
        return getInputPrice(amountIn,
                             fromToken.balanceOf(getAddress()),
                             toToken.balanceOf(getAddress()));
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
        return PRICE_FACTOR.multiply(fromToken.balanceOf(getAddress()))
                           .divide(toToken.balanceOf(getAddress()));
    }

    @Override
    protected void doInitialize() {
        sync();
    }

    @Override
    protected void getContractData() {
        wlock.lock();
        try {
            IToken trx = (IToken) getTokens().get(0);
            IToken token = (IToken) getTokens().get(1);
            trx.setBalance(getAddress(), trx.balanceOfFromChain(getAddress()));
            token.setBalance(getAddress(), token.balanceOfFromChain(getAddress()));
            log.info("{} balance {}", trx.getSymbol(), trx.balanceOf(getAddress()));
            log.info("{} balance {}", token.getSymbol(), token.balanceOf(getAddress()));
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
            case "Snapshot":
                return diffBalances();
            default:
                return false;
        }
    }

    @Override
    protected void handleEvent(String eventName, EventValues eventValues, long eventTime) {
        log.info("Diff {} {}", eventName, getAddress());
        switch (eventName) {
            case "Snapshot":
                handleSnapshotEvent(eventValues);
                break;
            default:
                log.warn("Ignore event {}", eventName);
                break;
        }
    }

    @Override
    protected ContractAbi loadAbi() {
        return tronContractTrigger.contractAt(SunswapV1Abi.class, getAddress());
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

    public BigInteger getKLast() {
        IToken token0 = (IToken) getTokens().get(0);
        IToken token1 = (IToken) getTokens().get(1);
        rlock.lock();
        try {
            return token0.balanceOf(getAddress()).multiply(token1.balanceOf(getAddress()));
        } finally {
            rlock.unlock();
        }
    }

    private boolean diffBalances() {
        log.info("diffBalances {}", getAddress());
        IToken trx = (IToken) getTokens().get(0);
        IToken token = (IToken) getTokens().get(1);
        BigInteger expectBalance0 = trx.balanceOfFromChain(getAddress());
        BigInteger expectBalance1 = token.balanceOfFromChain(getAddress());
        BigInteger localBalance0;
        BigInteger localBalance1;
        rlock.lock();
        try {
            localBalance0 = trx.balanceOf(getAddress());
            localBalance1 = token.balanceOf(getAddress());
        } finally {
            rlock.unlock();
        }
        if (0 != localBalance0.compareTo(expectBalance0) ||
            0 != localBalance1.compareTo(expectBalance1)) {
            log.error("expect balance0 {}", expectBalance0);
            log.error("expect balance1 {}", expectBalance1);
            log.error("local balance0 {}", localBalance0);
            log.error("local balance1 {}", localBalance1);
            return true;
        }
        log.trace("current balance0 {}", expectBalance0);
        log.trace("current balance1 {}", expectBalance1);
        return false;
    }

    private BigInteger getInputPrice(BigInteger amountIn,
                                     BigInteger balanceIn,
                                     BigInteger balanceOut) {
        BigInteger amountInWithFee = amountIn.multiply(FEE_NUMERATOR);
        BigInteger amountOut = amountInWithFee.multiply(balanceOut)
                                              .divide(balanceIn.multiply(FEE_DENOMINATOR)
                                                               .add(amountInWithFee));
        // amountOut never reach balanceOut
        return amountOut;
    }

    private void handleSnapshotEvent(EventValues eventValues) {
        log.info("handleSnapshotEvent {}", getAddress());
        BigInteger balance0 = ((Uint256) eventValues.getIndexedValues().get(1)).getValue();
        BigInteger balance1 = ((Uint256) eventValues.getIndexedValues().get(2)).getValue();
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
