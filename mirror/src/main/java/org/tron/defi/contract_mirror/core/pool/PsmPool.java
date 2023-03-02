package org.tron.defi.contract_mirror.core.pool;

import lombok.extern.slf4j.Slf4j;
import org.tron.defi.contract.abi.ContractAbi;
import org.tron.defi.contract.abi.pool.PsmAbi;
import org.tron.defi.contract_mirror.core.Contract;
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
    private final String polyAddress;
    private PsmPoly poly;
    private PsmPoly.PsmInfo info;
    private BigInteger gemToUsddDecimalFactor;

    public PsmPool(String address, String polyAddress) {
        super(address);
        this.polyAddress = polyAddress;
        type = PoolType.PSM;
    }

    @Override
    public void init() {
        tokens.add((Contract) getUsddFromChain());
        tokens.add((Contract) getGemFromChain());
        gemToUsddDecimalFactor = BigInteger.valueOf(10)
                                           .pow(((IToken) tokens.get(0)).getDecimals() -
                                                ((IToken) tokens.get(1)).getDecimals());
        updateName();
        initPoly();
        sync();
    }

    @Override
    protected void getContractData() {
        wlock.lock();
        try {
            info = poly.getInfo(getAddress());
            getGem().setBalance(getAddress(), info.getBalanceGem());
            getUsdd().setBalance(getAddress(), info.getBalanceUsdd());
        } finally {
            wlock.unlock();
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
                log.warn("Ignore event " + eventName);
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
        BigInteger gemAmount = ((Uint256) eventValues.getNonIndexedValues().get(0)).getValue();
        BigInteger fee = ((Uint256) eventValues.getNonIndexedValues().get(1)).getValue();
        BigInteger usddAmount = gemAmount.multiply(gemToUsddDecimalFactor).add(fee);
        wlock.lock();
        try {
            info.setBalanceGem(TokenMath.safeSubtract(info.getBalanceGem(), gemAmount));
            info.setBalanceUsdd(TokenMath.safeAdd(info.getBalanceUsdd(), usddAmount));
            getGem().setBalance(getAddress(), info.getBalanceGem());
            getUsdd().setBalance(getAddress(), info.getBalanceUsdd());

            info.setAmountUsddToGem(TokenMath.safeAdd(info.getAmountUsddToGem(), usddAmount));
        } finally {
            wlock.unlock();
        }
    }

    private void handleFileEvent(EventValues eventValues) {
        String what = new String(((Bytes32) eventValues.getIndexedValues().get(0)).getValue());
        BigInteger value = ((Uint256) eventValues.getNonIndexedValues().get(0)).getValue();
        wlock.lock();
        try {
            switch (what) {
                case "tin":
                    info.setFeeToUsdd(value);
                    break;
                case "tout":
                    info.setFeeToGem(value);
                    break;
                case "quota":
                    // do nothing
                    break;
                default:
                    throw new IllegalArgumentException("UNKNOWN PARAMETER " + what);
            }
        } finally {
            wlock.unlock();
        }
    }

    private void handleSellGemEvent(EventValues eventValues) {
        BigInteger gemAmount = ((Uint256) eventValues.getNonIndexedValues().get(0)).getValue();
        BigInteger fee = ((Uint256) eventValues.getNonIndexedValues().get(1)).getValue();
        BigInteger usddAmount = gemAmount.multiply(gemToUsddDecimalFactor).subtract(fee);
        wlock.lock();
        try {
            info.setBalanceGem(TokenMath.safeAdd(info.getBalanceGem(), gemAmount));
            info.setBalanceUsdd(TokenMath.safeSubtract(info.getBalanceUsdd(), usddAmount));
            getGem().setBalance(getAddress(), info.getBalanceGem());
            getUsdd().setBalance(getAddress(), info.getBalanceUsdd());

            info.setAmountGemToUsdd(TokenMath.safeAdd(info.getAmountGemToUsdd(), gemAmount));
            info.setAmountTotalToUsdd(TokenMath.safeAdd(info.getAmountTotalToUsdd(), gemAmount));
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
}
