package org.tron.defi.contract_mirror.core.pool;

import lombok.extern.slf4j.Slf4j;
import org.tron.defi.contract.abi.ContractAbi;
import org.tron.defi.contract.abi.pool.CurveAbi;
import org.tron.defi.contract_mirror.core.Contract;
import org.tron.defi.contract_mirror.core.token.SSPLiquidityToken;
import org.tron.defi.contract_mirror.core.token.TRC20;
import org.tron.defi.contract_mirror.core.token.Token;
import org.tron.defi.contract_mirror.utils.chain.AddressConverter;
import org.web3j.abi.EventValues;
import org.web3j.abi.datatypes.Address;
import org.web3j.abi.datatypes.DynamicArray;
import org.web3j.abi.datatypes.NumericType;
import org.web3j.abi.datatypes.Type;
import org.web3j.abi.datatypes.generated.Int128;
import org.web3j.abi.datatypes.generated.Uint256;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
public class CurvePool extends Pool {
    private static final ArrayList<PoolType> CURVE_TYPE
        = new ArrayList<>(Arrays.asList(PoolType.CURVE2, PoolType.CURVE3));
    private static final BigInteger FEE_DENOMINATOR = new BigInteger(String.valueOf(Math.exp(10)));
    private static final BigInteger PRECISION = new BigInteger(String.valueOf(Math.exp(18)));
    private final List<BigInteger> RATES;
    private final int FEE_INDEX;
    private final List<BigInteger> balances;
    private BigInteger fee;
    private BigInteger adminFee;
    private BigInteger initialA;
    private long timeInitialA;
    private BigInteger futureA;
    private long timeFutureA;

    public CurvePool(String address, PoolType type) {
        super(address);
        int index = CURVE_TYPE.indexOf(type);
        if (index < 0) {
            throw new IllegalArgumentException("Unexpected type: " + type.name());
        }
        this.type = type;
        switch (type) {
            case CURVE2:
                RATES = Arrays.asList(new BigInteger("1000000000000000000000000000000"),
                                      new BigInteger("1000000000000000000"));
                FEE_INDEX = 2;
                balances = Arrays.asList(BigInteger.ZERO, BigInteger.ZERO);
                break;
            case CURVE3:
                RATES = Arrays.asList(new BigInteger("1000000000000000000"),
                                      new BigInteger("1000000000000000000"),
                                      new BigInteger("1000000000000000000000000000000"));
                FEE_INDEX = 3;
                balances = Arrays.asList(BigInteger.ZERO, BigInteger.ZERO, BigInteger.ZERO);
                break;
            default:
                throw new IllegalArgumentException("UNEXPECTED TYPE " + type.name());
        }
    }

    @Override
    public void init() {
        int n = getN();
        tokens.ensureCapacity(n);
        for (int i = 0; i < n; i++) {
            tokens.add(getTokenFromChain(i));
        }
        lpToken = getLpTokenFromChain();
        updateName();
        sync();
    }

    @Override
    protected void getContractData() {
        List<Type> response = abi.invoke(CurveAbi.Functions.FEE, Collections.emptyList());
        fee = ((Uint256) response.get(0)).getValue();
        response = abi.invoke(CurveAbi.Functions.ADMIN_FEE, Collections.emptyList());
        adminFee = ((Uint256) response.get(0)).getValue();

        response = abi.invoke(CurveAbi.Functions.INITIAL_A, Collections.emptyList());
        initialA = ((Uint256) response.get(0)).getValue();
        response = abi.invoke(CurveAbi.Functions.INITIAL_A_TIME, Collections.emptyList());
        timeInitialA = ((Uint256) response.get(0)).getValue().longValue();
        response = abi.invoke(CurveAbi.Functions.FUTURE_A, Collections.emptyList());
        futureA = ((Uint256) response.get(0)).getValue();
        response = abi.invoke(CurveAbi.Functions.FUTURE_A_TIME, Collections.emptyList());
        timeFutureA = ((Uint256) response.get(0)).getValue().longValue();

        SSPLiquidityToken sspLpToken = (SSPLiquidityToken) getLpToken();
        sspLpToken.setTotalSupply(sspLpToken.getTotalSupplyFromChain());
        for (int i = 0; i < getN(); i++) {
            response = abi.invoke(CurveAbi.Functions.BALANCES, Collections.singletonList(i));
            balances.set(i, ((Uint256) response.get(i)).getValue());

            TRC20 token = (TRC20) getTokens().get(i);
            token.setBalance(getAddress(), token.balanceOfFromChain(getAddress()));
        }
    }

    @Override
    protected void handleEvent(String eventName, EventValues eventValues, long eventTime) {
        switch (eventName) {
            case "TokenExchange":
                checkEventTimestamp(eventTime);
                handleTokenExchangeEvent(eventValues, eventTime);
                break;
            case "AddLiquidity":
                checkEventTimestamp(eventTime);
                handleAddLiquidityEvent(eventValues);
                break;
            case "RemoveLiquidity":
                checkEventTimestamp(eventTime);
                handleRemoveLiquidity(eventValues);
                break;
            case "RemoveLiquidityOne":
                // event can't handle
                throw new IllegalStateException();
            case "RemoveLiquidityImbalance":
                checkEventTimestamp(eventTime);
                handleRemoveLiquidityImbalance(eventValues);
                break;
            case "NewFee":
                handleNewFeeEvent(eventValues);
                break;
            case "RampA":
                handleRampAEvent(eventValues);
                break;
            case "StopRampA":
                handleStopRampAEvent(eventValues);
                break;
        }
    }

    @Override
    protected ContractAbi loadAbi() {
        return tronContractTrigger.contractAt(CurveAbi.class, getAddress());
    }

    public int getN() {
        final int CURVE_TOKENS_MIN = 2;
        return tokens.size() > 0 ? tokens.size() : CURVE_TOKENS_MIN + CURVE_TYPE.indexOf(type);
    }

    public BigInteger getVirtualPrice(long timestamp) {
        return getD(getXP(), getA(timestamp)).multiply(PRECISION)
                                             .divide(((SSPLiquidityToken) getLpToken()).totalSupply());
    }

    private BigInteger getA(long timestamp) {
        if (timestamp < timeFutureA) {
            if (futureA.subtract(initialA).compareTo(BigInteger.ZERO) > 0) {
                return initialA.add(futureA.subtract(initialA)
                                           .multiply(BigInteger.valueOf(timestamp - timeInitialA))
                                           .divide(BigInteger.valueOf(timeFutureA - timeInitialA)));
            } else {
                return initialA.subtract(initialA.subtract(futureA)
                                                 .multiply(BigInteger.valueOf(timestamp -
                                                                              timeInitialA))
                                                 .divide(BigInteger.valueOf(timeFutureA -
                                                                            timeInitialA)));
            }
        } else {
            return futureA;
        }
    }

    private BigInteger getD(List<BigInteger> xp, BigInteger A) {
        BigInteger S = new BigInteger("0");
        for (BigInteger xi : xp) {
            S = S.add(xi);
        }
        if (S.compareTo(BigInteger.ZERO) == 0) {
            return BigInteger.ZERO;
        }
        BigInteger prevD;
        BigInteger D = S;
        BigInteger N = BigInteger.valueOf(xp.size());
        BigInteger Ann = A.multiply(N);
        final int MAX_LOOP = 255;
        for (int i = 0; i < MAX_LOOP; i++) {
            BigInteger D_P = D;
            for (BigInteger xi : xp) {
                D_P = D_P.multiply(D).divide(xi.multiply(N));
            }
            prevD = D;
            D = Ann.multiply(S)
                   .add(D_P.multiply(N))
                   .multiply(D)
                   .divide(Ann.subtract(BigInteger.ONE)
                              .multiply(D)
                              .add(N.add(BigInteger.ONE).multiply(D_P)));
            if (D.subtract(prevD).abs().compareTo(BigInteger.ONE) <= 0) {
                break;
            }
        }
        return D;
    }

    private Token getLpTokenFromChain() {
        List<Type> response = abi.invoke(CurveAbi.Functions.TOKEN, Collections.emptyList());
        String tokenAddress
            = AddressConverter.EthToTronBase58Address(((Address) response.get(0)).getValue());
        Contract contract = contractManager.getContract(tokenAddress);
        return contract != null
               ? (Token) contract
               : (Token) contractManager.registerContract(new SSPLiquidityToken(tokenAddress));
    }

    private Token getTokenFromChain(int n) {
        List<Type> response = abi.invoke(CurveAbi.Functions.COINS, Collections.singletonList(n));
        String tokenAddress
            = AddressConverter.EthToTronBase58Address(((Address) response.get(0)).getValue());
        Contract contract = contractManager.getContract(tokenAddress);
        return contract != null
               ? (Token) contract
               : (Token) contractManager.registerContract(new TRC20(tokenAddress));
    }

    private List<BigInteger> getXP() {
        List<BigInteger> xp = new ArrayList<>(balances.size());
        for (int i = 0; i < balances.size(); i++) {
            xp.add(balances.get(i).multiply(RATES.get(i)).divide(PRECISION));
        }
        return xp;
    }

    private BigInteger getY(int i, int j, BigInteger x, List<BigInteger> xp, BigInteger A) {
        BigInteger N = BigInteger.valueOf(xp.size());
        BigInteger D = getD(xp, A);
        BigInteger Ann = A.multiply(N);
        BigInteger S = BigInteger.ZERO;
        BigInteger C = D;
        for (int k = 0; k < xp.size(); k++) {
            if (k == j) {
                continue;
            }
            BigInteger x_k = k == i ? x : xp.get(k);
            S = S.add(x_k);
            C = C.multiply(D).divide(x_k.multiply(N));
        }
        C = C.multiply(D).divide(Ann.multiply(N));
        BigInteger b = S.add(D.divide(Ann));
        /*
         TODO: replace Newton's method with Vieta's formula
         return D.subtract(b)
                 .add(b.subtract(D).pow(2).add(BigInteger.valueOf(4).multiply(C)).sqrt())
                 .divide(BigInteger.TWO);
         */
        BigInteger prevY;
        BigInteger Y = D;
        final int MAX_LOOP = 255;
        for (int k = 0; k < MAX_LOOP; k++) {
            prevY = Y;
            Y = Y.multiply(Y).add(C).divide(Y.multiply(BigInteger.TWO).add(b).subtract(D));
            if (Y.subtract(prevY).abs().compareTo(BigInteger.ONE) <= 0) {
                break;
            }
        }
        return Y;
    }

    private void handleAddLiquidityEvent(EventValues eventValues) {
        String provider
            = AddressConverter.EthToTronBase58Address(((Address) eventValues.getIndexedValues()
                                                                            .get(0)).getValue());
        List<BigInteger> amounts = ((DynamicArray<Uint256>) eventValues.getNonIndexedValues()
                                                                       .get(0)).getValue()
                                                                               .stream()
                                                                               .map(NumericType::getValue)
                                                                               .collect(Collectors.toList());
        List<BigInteger> fees = ((DynamicArray<Uint256>) eventValues.getNonIndexedValues()
                                                                    .get(1)).getValue()
                                                                            .stream()
                                                                            .map(NumericType::getValue)
                                                                            .collect(Collectors.toList());
        BigInteger tokenSupply = ((Uint256) eventValues.getNonIndexedValues().get(3)).getValue();

        if (amounts.size() != getN() || amounts.size() != fees.size()) {
            throw new IllegalArgumentException("SIZE NOT MATCH");
        }
        SSPLiquidityToken sspLpToken = (SSPLiquidityToken) getLpToken();
        if (sspLpToken.totalSupply().compareTo(BigInteger.ZERO) > 0) {
            for (int i = 0; i < getN(); i++) {
                TRC20 token = (TRC20) getTokens().get(i);
                BigInteger balanceN = token.balanceOf(getAddress());
                token.setBalance(getAddress(), balanceN.add(amounts.get(i)));

                BigInteger adminFeeN = fees.get(i).multiply(adminFee).divide(FEE_DENOMINATOR);
                balances.set(i, balances.get(i).add(amounts.get(i)).subtract(adminFeeN));
            }
        } else {
            for (int i = 0; i < getN(); i++) {
                TRC20 token = (TRC20) getTokens().get(i);
                BigInteger balanceN = token.balanceOf(getAddress());
                token.setBalance(getAddress(), balanceN.add(amounts.get(i)));

                balances.set(i, balances.get(i).add(amounts.get(i)));
            }
        }
        BigInteger amountMint = tokenSupply.subtract(sspLpToken.totalSupply());
        sspLpToken.setBalance(provider, sspLpToken.balanceOf(provider).add(amountMint));
        sspLpToken.setTotalSupply(tokenSupply);
    }

    private void handleNewFeeEvent(EventValues eventValues) {
        fee = ((Uint256) eventValues.getNonIndexedValues().get(0)).getValue();
        adminFee = ((Uint256) eventValues.getNonIndexedValues().get(1)).getValue();
    }

    private void handleRampAEvent(EventValues eventValues) {
        initialA = ((Uint256) eventValues.getNonIndexedValues().get(0)).getValue();
        futureA = ((Uint256) eventValues.getNonIndexedValues().get(1)).getValue();
        timeInitialA = ((Uint256) eventValues.getNonIndexedValues().get(2)).getValue().longValue();
        timeFutureA = ((Uint256) eventValues.getNonIndexedValues().get(3)).getValue().longValue();
    }

    private void handleRemoveLiquidity(EventValues eventValues) {
        String provider
            = AddressConverter.EthToTronBase58Address(((Address) eventValues.getIndexedValues()
                                                                            .get(0)).getValue());
        List<BigInteger> amounts = ((DynamicArray<Uint256>) eventValues.getNonIndexedValues()
                                                                       .get(0)).getValue()
                                                                               .stream()
                                                                               .map(NumericType::getValue)
                                                                               .collect(Collectors.toList());
        BigInteger tokenSupply = ((Uint256) eventValues.getNonIndexedValues().get(2)).getValue();
        if (amounts.size() != getN()) {
            throw new IllegalArgumentException("SIZE NOT MATCH");
        }
        for (int i = 0; i < getN(); i++) {
            TRC20 token = (TRC20) getTokens().get(i);
            token.setBalance(getAddress(), token.balanceOf(getAddress()).subtract(amounts.get(i)));

            balances.set(i, balances.get(i).subtract(amounts.get(i)));
        }
        SSPLiquidityToken sspLpToken = (SSPLiquidityToken) getLpToken();
        BigInteger amountBurn = sspLpToken.totalSupply().subtract(tokenSupply);
        sspLpToken.setBalance(provider, sspLpToken.balanceOf(provider).subtract(amountBurn));
        sspLpToken.setTotalSupply(tokenSupply);
    }

    private void handleRemoveLiquidityImbalance(EventValues eventValues) {
        String provider
            = AddressConverter.EthToTronBase58Address(((Address) eventValues.getIndexedValues()
                                                                            .get(0)).getValue());
        List<BigInteger> amounts = ((DynamicArray<Uint256>) eventValues.getNonIndexedValues()
                                                                       .get(0)).getValue()
                                                                               .stream()
                                                                               .map(NumericType::getValue)
                                                                               .collect(Collectors.toList());
        List<BigInteger> fees = ((DynamicArray<Uint256>) eventValues.getNonIndexedValues()
                                                                    .get(1)).getValue()
                                                                            .stream()
                                                                            .map(NumericType::getValue)
                                                                            .collect(Collectors.toList());
        BigInteger tokenSupply = ((Uint256) eventValues.getNonIndexedValues().get(3)).getValue();

        if (amounts.size() != getN() || amounts.size() != fees.size()) {
            throw new IllegalArgumentException("SIZE NOT MATCH");
        }

        for (int i = 0; i < getN(); i++) {
            TRC20 token = (TRC20) getTokens().get(i);
            BigInteger balanceN = token.balanceOf(getAddress());
            token.setBalance(getAddress(), balanceN.subtract(amounts.get(i)));

            BigInteger adminFeeN = fees.get(i).multiply(adminFee).divide(FEE_DENOMINATOR);
            balances.set(i, balances.get(i).subtract(amounts.get(i)).subtract(adminFeeN));
        }
        SSPLiquidityToken sspLpToken = (SSPLiquidityToken) getLpToken();
        BigInteger amountBurn = sspLpToken.totalSupply().subtract(tokenSupply);
        sspLpToken.setBalance(provider, sspLpToken.balanceOf(provider).subtract(amountBurn));
        sspLpToken.setTotalSupply(tokenSupply);
    }

    private void handleStopRampAEvent(EventValues eventValues) {
        initialA = ((Uint256) eventValues.getNonIndexedValues().get(0)).getValue();
        timeInitialA = ((Uint256) eventValues.getNonIndexedValues().get(1)).getValue().longValue();
        futureA = initialA;
        timeFutureA = timeInitialA;
    }

    private void handleTokenExchangeEvent(EventValues eventValues, long eventTime) {
        int soldId = ((Int128) eventValues.getNonIndexedValues().get(0)).getValue().intValue();
        BigInteger amountSold = ((Uint256) eventValues.getNonIndexedValues().get(1)).getValue();
        int buyId = ((Int128) eventValues.getNonIndexedValues().get(2)).getValue().intValue();
        BigInteger amountBuy = ((Uint256) eventValues.getNonIndexedValues().get(3)).getValue();

        TRC20 tokenSold = (TRC20) getTokens().get(soldId);
        TRC20 tokenBuy = (TRC20) getTokens().get(buyId);
        tokenSold.setBalance(getAddress(), tokenSold.balanceOf(getAddress()).add(amountSold));
        tokenBuy.setBalance(getAddress(), tokenBuy.balanceOf(getAddress()).subtract(amountBuy));


        // dyFee = dy * fee / FEE_DENOMINATOR
        // amountBuy = (dy - dyFee) * PRECISION / RATES[j]
        // dyFee = amountBuy * RATES[j] * fee / PRECISION / (FEE_DENOMINATOR - fee)
        // dyAdminFee = dyFee * adminFee / FEE_DENOMINATOR * PRECISION / RATES[j]
        //            = amountBuy * fee * adminFee / FEE_DENOMINATOR / (FEE_DENOMINATOR - fee)
        BigInteger dyAdminFee = amountBuy.multiply(fee)
                                         .multiply(adminFee)
                                         .divide(FEE_DENOMINATOR)
                                         .divide(FEE_DENOMINATOR.subtract(fee));

        if (soldId != FEE_INDEX) {
            // It's easy when amountSold is equal to token received by contract
            balances.set(soldId, balances.get(soldId).add(amountSold));
            balances.set(buyId, balances.get(buyId).subtract(amountBuy).subtract(dyAdminFee));
            return;
        }
        // TODO: heavy computation workload in this case, assuming computation is more efficient
        //  than simply sync()
        // dyAdminFee =
        //   dy * fee / FEE_DENOMINATOR * adminFee / FEE_DENOMINATOR * PRECISION / RATES[buyId]
        BigInteger dy = dyAdminFee.multiply(FEE_DENOMINATOR)
                                  .multiply(FEE_DENOMINATOR)
                                  .multiply(RATES.get(buyId))
                                  .divide(fee)
                                  .divide(adminFee)
                                  .divide(PRECISION);
        // dy = xp[buyId] - y - 1
        List<BigInteger> xp = getXP();
        BigInteger y = xp.get(buyId).subtract(dy).subtract(BigInteger.ONE);
        // use y to cal x
        BigInteger A = getA(eventTime);
        BigInteger x = getY(buyId, soldId, y, xp, A);
        // x = xp[soldId] + dx * RATES[soldId] / PRECISION
        BigInteger dx = x.subtract(xp.get(soldId)).multiply(PRECISION).divide(RATES.get(soldId));
        balances.set(soldId, balances.get(soldId).add(dx));
        balances.set(buyId, balances.get(buyId).subtract(amountBuy).subtract(dyAdminFee));
    }
}
