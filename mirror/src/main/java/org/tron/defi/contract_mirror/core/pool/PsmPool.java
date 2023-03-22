package org.tron.defi.contract_mirror.core.pool;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.tron.defi.contract.abi.ContractAbi;
import org.tron.defi.contract.abi.pool.PsmAbi;
import org.tron.defi.contract_mirror.core.Contract;
import org.tron.defi.contract_mirror.core.PsmPoly;
import org.tron.defi.contract_mirror.core.token.ITRC20;
import org.tron.defi.contract_mirror.core.token.IToken;
import org.tron.defi.contract_mirror.core.token.TRC20;
import org.tron.defi.contract_mirror.utils.TokenMath;
import org.tron.defi.contract_mirror.utils.chain.AddressConverter;
import org.web3j.abi.EventValues;
import org.web3j.abi.datatypes.Address;
import org.web3j.abi.datatypes.Type;
import org.web3j.abi.datatypes.generated.Bytes32;
import org.web3j.abi.datatypes.generated.Uint256;

import java.math.BigInteger;
import java.util.Collections;
import java.util.List;

@Slf4j
public class PsmPool extends Pool {
    private static final BigInteger FEE_DENOMINATOR = new BigInteger("1000000000000000000");
    private final String polyAddress;
    private PsmPoly poly;
    private PsmPoly.PsmInfo info;
    @Getter
    private BigInteger gemToUsddDecimalFactor;

    public PsmPool(String address, String polyAddress) {
        super(address);
        this.polyAddress = polyAddress;
        type = PoolType.PSM;
    }

    @Override
    public boolean isReady() {
        if (!isEventAccept()) {
            return false;
        }
        if (ready) {
            return true;
        }
        ready = System.currentTimeMillis() > timestamp2;
        return ready;
    }

    @Override
    public BigInteger getAmountOutUnsafe(IToken fromToken, IToken toToken, BigInteger amountIn) {
        return fromToken == getUsdd() ? buyGemOut(amountIn) : sellGemOut(amountIn);
    }

    @Override
    public BigInteger getApproximateFee(IToken fromToken, IToken toToken, BigInteger amountIn) {
        rlock.lock();
        try {
            return fromToken == getUsdd()
                   ? amountIn.multiply(info.getFeeToGem())
                             .divide(FEE_DENOMINATOR)
                   : amountIn.multiply(info.getFeeToUsdd()).divide(FEE_DENOMINATOR);
        } finally {
            rlock.unlock();
        }
    }

    @Override
    public BigInteger getPrice(IToken fromToken, IToken toToken) {
        return fromToken == getUsdd()
               ? PRICE_FACTOR.multiply(gemToUsddDecimalFactor)
               : PRICE_FACTOR.divide(gemToUsddDecimalFactor);
    }

    @Override
    protected void doInitialize() {
        tokens.add((Contract) getUsddFromChain());
        tokens.add((Contract) getGemFromChain());
        gemToUsddDecimalFactor = BigInteger.valueOf(10)
                                           .pow(((IToken) tokens.get(0)).getDecimals() -
                                                ((IToken) tokens.get(1)).getDecimals());
        log.info("gemToUsddDecimalFactor = {}", gemToUsddDecimalFactor);
        initPoly();
        sync();
    }

    @Override
    protected void getContractData() {
        wlock.lock();
        try {
            info = poly.getInfo(getAddress());
            ((IToken) getGem()).setBalance(getAddress(), info.getBalanceGem());
            ((IToken) getUsdd()).setBalance(getAddress(), info.getBalanceUsdd());
            log.info("psm info {}", info);
        } finally {
            wlock.unlock();
        }
    }

    @Override
    public int cost() {
        return 0 == info.getFeeToGem().compareTo(BigInteger.ZERO) &&
               0 == info.getFeeToUsdd().compareTo(BigInteger.ZERO) ? 0 : 1;
    }

    @Override
    protected boolean doDiff(String eventName) {
        switch (eventName) {
            case "File":
            case "SellGem":
            case "BuyGem":
                log.info("Diff {} {}", eventName, getAddress());
                return diffAll();
            default:
                return false;
        }
    }

    @Override
    protected void handleEvent(String eventName, EventValues eventValues, long eventTime) {
        switch (eventName) {
            case "File":
                handleFileEvent(eventValues);
                break;
            case "SellGem":
                checkEventTimestamp(eventTime);
                handleSellGemEvent(eventValues);
                break;
            case "BuyGem":
                checkEventTimestamp(eventTime);
                handleBuyGemEvent(eventValues);
                break;
            default:
                log.warn("Ignore event {}", eventName);
                break;
        }
    }

    @Override
    protected ContractAbi loadAbi() {
        return tronContractTrigger.contractAt(PsmAbi.class, getAddress());
    }

    public ITRC20 getGem() {
        final int gemId = 1;
        return (ITRC20) tokens.get(gemId);
    }

    public ITRC20 getUsdd() {
        final int usddId = 0;
        return (ITRC20) tokens.get(usddId);
    }

    private BigInteger buyGemOut(BigInteger amountIn) {
        rlock.lock();
        try {
            BigInteger fee = amountIn.multiply(info.getFeeToGem()).divide(FEE_DENOMINATOR);
            BigInteger amountOut = amountIn.subtract(fee).divide(gemToUsddDecimalFactor);
            if (info.getBalanceGem().compareTo(amountOut) < 0) {
                throw new RuntimeException(getName() + " NOT ENOUGH BALANCE");
            }
            if (info.getEnableUsddToGemQuota() &&
                info.getQuotaUsddToGem().compareTo(amountOut) < 0) {
                throw new RuntimeException(getName() + " NOT ENOUGH QUOTA");
            }
            return amountOut;
        } finally {
            rlock.unlock();
        }
    }

    private boolean diffAll() {
        PsmPoly.PsmInfo current = poly.getInfo(getAddress());
        rlock.lock();
        try {
            if (!info.equals(current)) {
                log.error("expect {}", current);
                log.error("actual {}", info);
                return true;
            }
            return false;
        } finally {
            rlock.unlock();
        }
    }

    private ITRC20 getGemFromChain() {
        List<Type> response = abi.invoke(PsmAbi.Functions.GEM_JOIN, Collections.emptyList());
        String gemJoinAddress
            = AddressConverter.EthToTronBase58Address(((Address) response.get(0)).getValue());
        response = abi.invoke(gemJoinAddress, PsmAbi.Functions.GEM, Collections.emptyList());
        String tokenAddress
            = AddressConverter.EthToTronBase58Address(((Address) response.get(0)).getValue());
        Contract contract = contractManager.getContract(tokenAddress);
        return null != contract
               ? (ITRC20) contract
               : (ITRC20) contractManager.registerContract(new TRC20(tokenAddress));
    }

    private ITRC20 getUsddFromChain() {
        List<Type> response = abi.invoke(PsmAbi.Functions.USDD, Collections.emptyList());
        String tokenAddress
            = AddressConverter.EthToTronBase58Address(((Address) response.get(0)).getValue());
        Contract contract = contractManager.getContract(tokenAddress);
        return null != contract
               ? (ITRC20) contract
               : (ITRC20) contractManager.registerContract(new TRC20(tokenAddress));
    }

    private void handleBuyGemEvent(EventValues eventValues) {
        log.info("handleBuyGemEvent {}", getAddress());
        BigInteger gemAmount = ((Uint256) eventValues.getNonIndexedValues().get(0)).getValue();
        BigInteger fee = ((Uint256) eventValues.getNonIndexedValues().get(1)).getValue();
        BigInteger usddAmount = gemAmount.multiply(gemToUsddDecimalFactor).add(fee);
        wlock.lock();
        try {
            BigInteger balanceBefore = info.getBalanceGem();
            BigInteger balanceAfter = TokenMath.safeSubtract(balanceBefore, gemAmount);
            info.setBalanceGem(balanceAfter);
            log.info("Gem balance {} -> {}", balanceBefore, balanceAfter);

            balanceBefore = info.getBalanceUsdd();
            balanceAfter = TokenMath.safeAdd(balanceBefore, usddAmount);
            info.setBalanceUsdd(balanceAfter);
            log.info("USDD balance {} -> {}", balanceBefore, balanceAfter);

            ((IToken) getGem()).setBalance(getAddress(), info.getBalanceGem());
            ((IToken) getUsdd()).setBalance(getAddress(), info.getBalanceUsdd());
            if (info.getEnableUsddToGemQuota()) {
                balanceBefore = info.getQuotaUsddToGem();
                balanceAfter = TokenMath.safeSubtract(balanceBefore, usddAmount);
                info.setQuotaUsddToGem(balanceAfter);
                log.info("QuotaUsddToGem {} -> {}", balanceBefore, balanceAfter);
            }
        } finally {
            wlock.unlock();
        }
    }

    private void handleFileEvent(EventValues eventValues) {
        log.info("handleFileEvent {}", getAddress());
        String what = new String(((Bytes32) eventValues.getIndexedValues().get(0)).getValue());
        BigInteger value = ((Uint256) eventValues.getNonIndexedValues().get(0)).getValue();
        wlock.lock();
        try {
            switch (what) {
                case "tin":
                    log.info("feeToUsdd {} -> {}", info.getFeeToUsdd(), value);
                    info.setFeeToUsdd(value);
                    break;
                case "tout":
                    log.info("feeToGem {} -> {}", info.getFeeToGem(), value);
                    info.setFeeToGem(value);
                    break;
                case "quota":
                    // do nothing
                    throw new IllegalStateException("CAN NOT HANDLE QUOTA");
                default:
                    log.warn("UNKNOWN PARAMETER {}", what);
            }
        } finally {
            wlock.unlock();
        }
    }

    private void handleSellGemEvent(EventValues eventValues) {
        log.info("handleSellGemEvent {}", getAddress());
        BigInteger gemAmount = ((Uint256) eventValues.getNonIndexedValues().get(0)).getValue();
        BigInteger fee = ((Uint256) eventValues.getNonIndexedValues().get(1)).getValue();
        BigInteger usddAmount = gemAmount.multiply(gemToUsddDecimalFactor).subtract(fee);
        wlock.lock();
        try {
            BigInteger balanceBefore = info.getBalanceGem();
            BigInteger balanceAfter = TokenMath.safeAdd(balanceBefore, gemAmount);
            info.setBalanceGem(balanceAfter);
            log.info("Gem balance {} -> {}", balanceBefore, balanceAfter);

            balanceBefore = info.getBalanceUsdd();
            balanceAfter = TokenMath.safeSubtract(balanceBefore, usddAmount);
            info.setBalanceUsdd(balanceAfter);
            log.info("USDD balance {} -> {}", balanceBefore, balanceAfter);

            ((IToken) getGem()).setBalance(getAddress(), info.getBalanceGem());
            ((IToken) getUsdd()).setBalance(getAddress(), info.getBalanceUsdd());

            balanceBefore = info.getAmountGemToUsdd();
            balanceAfter = TokenMath.safeAdd(balanceBefore, gemAmount);
            info.setAmountGemToUsdd(balanceAfter);
            log.info("AmountGemToUsdd {} -> {}", balanceBefore, balanceAfter);

            balanceBefore = info.getAmountTotalToUsdd();
            balanceAfter = TokenMath.safeAdd(balanceBefore, gemAmount);
            info.setAmountTotalToUsdd(balanceAfter);
            log.info("AmountTotalToUsdd {} -> {}", balanceBefore, balanceAfter);
        } finally {
            wlock.unlock();
        }
    }

    private void initPoly() {
        Contract contract = contractManager.getContract(polyAddress);
        poly = null != contract
               ? (PsmPoly) contract
               : (PsmPoly) contractManager.registerContract(new PsmPoly(polyAddress));
    }

    private BigInteger sellGemOut(BigInteger amountIn) {
        rlock.lock();
        try {
            amountIn = amountIn.multiply(gemToUsddDecimalFactor);
            BigInteger fee = amountIn.multiply(info.getFeeToUsdd()).divide(FEE_DENOMINATOR);
            BigInteger amountOut = amountIn.subtract(fee);
            if (info.getBalanceUsdd().compareTo(amountOut) < 0) {
                throw new RuntimeException(getName() + " NOT ENOUGH BALANCE");
            }
            if (TokenMath.safeAdd(info.getAmountGemToUsdd(), amountOut)
                         .compareTo(info.getQuotaGemToUsdd()) > 0 ||
                TokenMath.safeAdd(info.getAmountTotalToUsdd(), amountOut)
                         .compareTo(info.getQuotaTotalToUsdd()) > 0) {
                throw new RuntimeException(getName() + " NOT ENOUGH QUOTA");
            }
            return amountOut;
        } finally {
            rlock.unlock();
        }
    }
}
