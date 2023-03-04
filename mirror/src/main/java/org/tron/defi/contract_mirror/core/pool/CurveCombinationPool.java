package org.tron.defi.contract_mirror.core.pool;

import com.alibaba.fastjson.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.tron.defi.contract.abi.ContractAbi;
import org.tron.defi.contract.abi.pool.Curve4Abi;
import org.tron.defi.contract_mirror.core.Contract;
import org.tron.defi.contract_mirror.core.token.ITRC20;
import org.tron.defi.contract_mirror.core.token.IToken;
import org.tron.defi.contract_mirror.core.token.TRC20;
import org.tron.defi.contract_mirror.utils.TokenMath;
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
public class CurveCombinationPool extends Pool {
    private static final int N_COINS = 2;
    private static final int TOKEN_ID = 0;
    private static final int LP_TOKEN_ID = 1;
    private static final BigInteger FEE_DENOMINATOR = new BigInteger("10000000000");
    private static final BigInteger PRECISION = new BigInteger("1000000000000000000");
    private static final BigInteger A_PRECISION = new BigInteger("100");
    private static final List<BigInteger> RATES = Arrays.asList(new BigInteger(
        "1000000000000000000000000000000"), new BigInteger("1000000000000000000"));
    private static final int FEE_INDEX = 3;
    private static final long CACHE_EXPIRE_TIME = 10 * 60; // 10 min
    private List<BigInteger> balances;
    private BigInteger fee;
    private BigInteger adminFee;
    private BigInteger initialA;
    private long timeInitialA;
    private BigInteger futureA;
    private long timeFutureA;
    private BigInteger virtualPrice;
    private long timeUpdateVirtualPrice;
    private CurvePool underlyingPool;

    public CurveCombinationPool(String address, PoolType type) {
        super(address);
        this.type = type;
    }

    @Override
    public JSONObject getInfo() {
        JSONObject info = super.getInfo();
        info.put("base_pool", underlyingPool.getInfo());
        return info;
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
    protected void doInitialize() {
        initUnderlyingPool();
        tokens.add((Contract) getTokenFromChain(TOKEN_ID));
        tokens.addAll(underlyingPool.getTokens());
        ITRC20 underlyingLpToken = getTokenFromChain(LP_TOKEN_ID);
        balances = Arrays.asList(BigInteger.ZERO, BigInteger.ZERO);
        if (!underlyingLpToken.equals(underlyingPool.getLpToken())) {
            throw new IllegalArgumentException("UNDERLYING LP_TOKEN MISMATCH");
        }
        lpToken = getLpTokenFromChain();
        updateName();
        sync();
    }

    @Override
    protected void getContractData() {
        log.info("getContractData {}", getAddress());
        wlock.lock();
        try {
            List<Type> response = abi.invoke(Curve4Abi.Functions.FEE, Collections.emptyList());
            fee = ((Uint256) response.get(0)).getValue();
            response = abi.invoke(Curve4Abi.Functions.ADMIN_FEE, Collections.emptyList());
            adminFee = ((Uint256) response.get(0)).getValue();
            log.info("fee = {}", fee);
            log.info("adminFee = {}", adminFee);

            response = abi.invoke(Curve4Abi.Functions.INITIAL_A, Collections.emptyList());
            initialA = ((Uint256) response.get(0)).getValue();
            response = abi.invoke(Curve4Abi.Functions.INITIAL_A_TIME, Collections.emptyList());
            timeInitialA = ((Uint256) response.get(0)).getValue().longValue();
            response = abi.invoke(Curve4Abi.Functions.FUTURE_A, Collections.emptyList());
            futureA = ((Uint256) response.get(0)).getValue();
            response = abi.invoke(Curve4Abi.Functions.FUTURE_A_TIME, Collections.emptyList());
            timeFutureA = ((Uint256) response.get(0)).getValue().longValue();
            log.info("initialA = {}", initialA);
            log.info("timeInitialA = {}", timeInitialA);
            log.info("futureA = {}", futureA);
            log.info("timeFutureA = {}", timeFutureA);

            response = abi.invoke(Curve4Abi.Functions.BASE_VIRTUAL_PRICE, Collections.emptyList());
            virtualPrice = ((Uint256) response.get(0)).getValue();
            response = abi.invoke(Curve4Abi.Functions.BASE_CACHE_UPDATED, Collections.emptyList());
            timeUpdateVirtualPrice = ((Uint256) response.get(0)).getValue().longValue();
            log.info("virtualPrice = {}", virtualPrice);
            log.info("timeUpdateVirtualPrice = {}", timeUpdateVirtualPrice);

            getLpToken().setTotalSupply(getLpToken().getTotalSupplyFromChain());
            log.info("totalSupply = {}", getLpToken().totalSupply());

            for (int i = 0; i < N_COINS; i++) {
                response = abi.invoke(Curve4Abi.Functions.BALANCES, Collections.singletonList(i));
                balances.set(i, ((Uint256) response.get(0)).getValue());
                log.info("balance{} = {}", i, balances.get(i));

                IToken token = (IToken) getTokens().get(i);
                token.setBalance(getAddress(), token.balanceOfFromChain(getAddress()));
                log.info("{} balance {}", token.getSymbol(), token.balanceOf(getAddress()));
            }
        } finally {
            wlock.unlock();
        }
    }

    @Override
    protected void handleEvent(String eventName, EventValues eventValues, long eventTime) {
        switch (eventName) {
            case "TokenExchange":
                checkEventTimestamp(eventTime);
                handleTokenExchangeEvent(eventValues);
                break;
            case "TokenExchangeUnderlying":
                handleTokenExchangeUnderlyingEvent(eventValues, eventTime);
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
            default:
                log.warn("Ignore event " + eventName);
                break;
        }
    }

    @Override
    protected ContractAbi loadAbi() {
        return tronContractTrigger.contractAt(Curve4Abi.class, getAddress());
    }

    private BigInteger getA(long timestamp) {
        rlock.lock();
        try {
            if (timestamp < timeFutureA) {
                if (futureA.subtract(initialA).compareTo(BigInteger.ZERO) > 0) {
                    return initialA.add(futureA.subtract(initialA)
                                               .multiply(BigInteger.valueOf(timestamp -
                                                                            timeInitialA))
                                               .divide(BigInteger.valueOf(timeFutureA -
                                                                          timeInitialA)));
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
        } finally {
            rlock.unlock();
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
                   .divide(A_PRECISION)
                   .add(D_P.multiply(N))
                   .multiply(D)
                   .divide(Ann.subtract(A_PRECISION)
                              .multiply(D)
                              .divide(A_PRECISION)
                              .add(N.add(BigInteger.ONE).multiply(D_P)));
            if (D.subtract(prevD).abs().compareTo(BigInteger.ONE) <= 0) {
                return D;
            }
        }
        throw new IllegalStateException();
    }

    private ITRC20 getLpTokenFromChain() {
        List<Type> response = abi.invoke(Curve4Abi.Functions.LP_TOKEN, Collections.emptyList());
        String tokenAddress
            = AddressConverter.EthToTronBase58Address(((Address) response.get(0)).getValue());
        Contract contract = contractManager.getContract(tokenAddress);
        return contract != null
               ? (ITRC20) contract
               : (ITRC20) contractManager.registerContract(new TRC20(tokenAddress));
    }

    private ITRC20 getTokenFromChain(int n) throws RuntimeException {
        if (n > 1) {
            throw new IndexOutOfBoundsException("n = " + n);
        }
        List<Type> response = abi.invoke(Curve4Abi.Functions.COINS, Collections.singletonList(n));
        String tokenAddress
            = AddressConverter.EthToTronBase58Address(((Address) response.get(0)).getValue());
        Contract contract = contractManager.getContract(tokenAddress);
        return contract != null
               ? (ITRC20) contract
               : (ITRC20) contractManager.registerContract(new TRC20(tokenAddress));
    }

    private BigInteger getVirtualPrice(long timestamp, boolean update) {
        if (timestamp <= timeUpdateVirtualPrice + CACHE_EXPIRE_TIME) {
            return virtualPrice;
        }
        BigInteger price = underlyingPool.getVirtualPrice(timestamp);
        if (update) {
            virtualPrice = price;
            timeUpdateVirtualPrice = timestamp;
        }
        return price;
    }

    private List<BigInteger> getXP(BigInteger price) {
        rlock.lock();
        try {
            List<BigInteger> xp = new ArrayList<>(balances.size());
            for (int i = 0; i < balances.size(); i++) {
                xp.add(balances.get(i)
                               .multiply(i != N_COINS - 1 ? RATES.get(i) : price)
                               .divide(PRECISION));
            }
            return xp;
        } finally {
            rlock.unlock();
        }
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
        C = C.multiply(D).multiply(A_PRECISION).divide(Ann.multiply(N));
        BigInteger b = S.add(D.multiply(A_PRECISION).divide(Ann));
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
                return Y;
            }
        }
        throw new IllegalStateException();
    }

    private void handleAddLiquidityEvent(EventValues eventValues) {
        log.info("handleAddLiquidityEvent {}", getAddress());
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

        if (amounts.size() != N_COINS || amounts.size() != fees.size()) {
            throw new IllegalArgumentException("SIZE NOT MATCH");
        }
        wlock.lock();
        try {
            if (getLpToken().totalSupply().compareTo(BigInteger.ZERO) > 0) {
                for (int i = 0; i < N_COINS; i++) {
                    IToken token = (IToken) getTokens().get(i);
                    BigInteger balanceBefore = token.balanceOf(getAddress());
                    BigInteger balanceAfter = TokenMath.increaseBalance(token,
                                                                        getAddress(),
                                                                        amounts.get(i));
                    log.info("{} balance {} -> {}", token.getSymbol(), balanceBefore, balanceAfter);

                    BigInteger adminFeeN = fees.get(i).multiply(adminFee).divide(FEE_DENOMINATOR);
                    balanceBefore = balances.get(i);
                    balanceAfter = TokenMath.safeSubtract(TokenMath.safeAdd(balanceBefore,
                                                                            amounts.get(i)),
                                                          adminFeeN);
                    balances.set(i, balanceAfter);
                    log.info("balance{} {} -> {}", i, balanceBefore, balanceAfter);
                }
            } else {
                for (int i = 0; i < N_COINS; i++) {
                    IToken token = (IToken) getTokens().get(i);
                    BigInteger balanceBefore = token.balanceOf(getAddress());
                    BigInteger balanceAfter = TokenMath.increaseBalance(token,
                                                                        getAddress(),
                                                                        amounts.get(i));
                    log.info("{} balance {} -> {}", token.getSymbol(), balanceBefore, balanceAfter);

                    balanceBefore = balances.get(i);
                    balanceAfter = TokenMath.safeAdd(balances.get(i), amounts.get(i));
                    balances.set(i, balanceAfter);
                    log.info("balance{} {} -> {}", i, balanceBefore, balanceAfter);
                }
            }
            BigInteger amountMint = TokenMath.safeSubtract(tokenSupply, getLpToken().totalSupply());
            IToken token = (IToken) getLpToken();
            BigInteger balanceBefore = token.balanceOf(provider);
            BigInteger balanceAfter = TokenMath.increaseBalance(token, provider, amountMint);
            getLpToken().setTotalSupply(tokenSupply);
            log.info("{} totalSupply {}, {} balance {} -> {}",
                     token.getSymbol(),
                     tokenSupply,
                     provider,
                     balanceBefore,
                     balanceAfter);
        } finally {
            wlock.unlock();
        }
    }

    private void handleNewFeeEvent(EventValues eventValues) {
        log.info("handleNewFeeEvent {}", getAddress());
        wlock.lock();
        try {
            log.info("fee old = {}", fee);
            log.info("adminFee old = {}", adminFee);
            fee = ((Uint256) eventValues.getNonIndexedValues().get(0)).getValue();
            adminFee = ((Uint256) eventValues.getNonIndexedValues().get(1)).getValue();
            log.info("fee = {}", fee);
            log.info("adminFee = {}", adminFee);
        } finally {
            wlock.unlock();
        }
    }

    private void handleRampAEvent(EventValues eventValues) {
        log.info("handleRampAEvent {}", getAddress());
        wlock.lock();
        try {
            log.info("initialA old = {}", initialA);
            log.info("timeInitialA old = {}", timeInitialA);
            log.info("futureA old = {}", futureA);
            log.info("timeFutureA old = {}", timeFutureA);
            initialA = ((Uint256) eventValues.getNonIndexedValues().get(0)).getValue();
            futureA = ((Uint256) eventValues.getNonIndexedValues().get(1)).getValue();
            timeInitialA = ((Uint256) eventValues.getNonIndexedValues().get(2)).getValue()
                                                                               .longValue();
            timeFutureA = ((Uint256) eventValues.getNonIndexedValues().get(3)).getValue()
                                                                              .longValue();
            log.info("initialA = {}", initialA);
            log.info("timeInitialA = {}", timeInitialA);
            log.info("futureA = {}", futureA);
            log.info("timeFutureA = {}", timeFutureA);
        } finally {
            wlock.unlock();
        }
    }

    private void handleRemoveLiquidity(EventValues eventValues) {
        log.info("handleRemoveLiquidity {}", getAddress());
        String provider
            = AddressConverter.EthToTronBase58Address(((Address) eventValues.getIndexedValues()
                                                                            .get(0)).getValue());
        List<BigInteger> amounts = ((DynamicArray<Uint256>) eventValues.getNonIndexedValues()
                                                                       .get(0)).getValue()
                                                                               .stream()
                                                                               .map(NumericType::getValue)
                                                                               .collect(Collectors.toList());
        BigInteger tokenSupply = ((Uint256) eventValues.getNonIndexedValues().get(2)).getValue();
        if (amounts.size() != N_COINS) {
            throw new IllegalArgumentException("SIZE NOT MATCH");
        }
        wlock.lock();
        try {
            for (int i = 0; i < N_COINS; i++) {
                IToken token = (IToken) getTokens().get(i);
                BigInteger balanceBefore = token.balanceOf(getAddress());
                BigInteger balanceAfter = TokenMath.decreaseBalance(token,
                                                                    getAddress(),
                                                                    amounts.get(i));
                log.info("{} balance {} -> {}", token.getSymbol(), balanceBefore, balanceAfter);

                balanceBefore = balances.get(i);
                balanceAfter = TokenMath.safeSubtract(balanceBefore, amounts.get(i));
                balances.set(i, balanceAfter);
                log.info("balance{} {} -> {}", i, balanceBefore, balanceAfter);
            }
            BigInteger amountBurn = TokenMath.safeSubtract(getLpToken().totalSupply(), tokenSupply);
            IToken token = (IToken) getLpToken();
            BigInteger balanceBefore = token.balanceOf(provider);
            BigInteger balanceAfter = TokenMath.decreaseBalance(token, provider, amountBurn);
            getLpToken().setTotalSupply(tokenSupply);
            log.info("{} totalSupply {}, {} balance {} -> {}",
                     token.getSymbol(),
                     tokenSupply,
                     provider,
                     balanceBefore,
                     balanceAfter);
        } finally {
            wlock.unlock();
        }
    }

    private void handleRemoveLiquidityImbalance(EventValues eventValues) {
        log.info("handleRemoveLiquidityImbalance {}", getAddress());
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

        if (amounts.size() != N_COINS || amounts.size() != fees.size()) {
            throw new IllegalArgumentException("SIZE NOT MATCH");
        }
        wlock.lock();
        try {
            for (int i = 0; i < N_COINS; i++) {
                IToken token = (IToken) getTokens().get(i);
                BigInteger balanceBefore = token.balanceOf(getAddress());
                BigInteger balanceAfter = TokenMath.decreaseBalance(token,
                                                                    getAddress(),
                                                                    amounts.get(i));
                log.info("{} balance {} -> {}", token.getSymbol(), balanceBefore, balanceAfter);

                BigInteger adminFeeN = fees.get(i).multiply(adminFee).divide(FEE_DENOMINATOR);
                balanceBefore = balances.get(i);
                balanceAfter = TokenMath.safeSubtract(balanceBefore,
                                                      TokenMath.safeAdd(amounts.get(i), adminFeeN));
                balances.set(i, balanceAfter);
                log.info("balance{} {} -> {}", i, balanceBefore, balanceAfter);
            }
            BigInteger amountBurn = TokenMath.safeSubtract(getLpToken().totalSupply(), tokenSupply);
            IToken token = (IToken) getLpToken();
            BigInteger balanceBefore = token.balanceOf(provider);
            BigInteger balanceAfter = TokenMath.decreaseBalance(token, provider, amountBurn);
            getLpToken().setTotalSupply(tokenSupply);
            log.info("{} totalSupply {}, {} balance {} -> {}",
                     token.getSymbol(),
                     tokenSupply,
                     provider,
                     balanceBefore,
                     balanceAfter);
        } finally {
            wlock.unlock();
        }
    }

    private void handleStopRampAEvent(EventValues eventValues) {
        log.info("handleStopRampAEvent {}", getAddress());
        wlock.unlock();
        try {
            initialA = ((Uint256) eventValues.getNonIndexedValues().get(0)).getValue();
            timeInitialA = ((Uint256) eventValues.getNonIndexedValues().get(1)).getValue()
                                                                               .longValue();
            futureA = initialA;
            timeFutureA = timeInitialA;
            log.info("initialA = {}", initialA);
            log.info("timeInitialA = {}", timeInitialA);
            log.info("futureA = {}", futureA);
            log.info("timeFutureA = {}", timeFutureA);
        } finally {
            wlock.unlock();
        }
    }

    private void handleTokenExchangeEvent(EventValues eventValues) {
        log.info("handleTokenExchangeEvent {}", getAddress());
        int soldId = ((Int128) eventValues.getNonIndexedValues().get(0)).getValue().intValue();
        BigInteger amountSold = ((Uint256) eventValues.getNonIndexedValues().get(1)).getValue();
        int buyId = ((Int128) eventValues.getNonIndexedValues().get(2)).getValue().intValue();
        BigInteger amountBuy = ((Uint256) eventValues.getNonIndexedValues().get(3)).getValue();

        // dyFee = dy * fee / FEE_DENOMINATOR
        // amountBuy = (dy - dyFee) * PRECISION / RATES[buyId]
        // dyFee = amountBuy * RATES[buyId] * fee / PRECISION / (FEE_DENOMINATOR - fee)
        // dyAdminFee = dyFee * adminFee / FEE_DENOMINATOR * PRECISION / RATES[buyId]
        //            = amountBuy * fee * adminFee / FEE_DENOMINATOR / (FEE_DENOMINATOR - fee)
        BigInteger dyAdminFee = amountBuy.multiply(fee)
                                         .multiply(adminFee)
                                         .divide(FEE_DENOMINATOR)
                                         .divide(FEE_DENOMINATOR.subtract(fee));
        wlock.lock();
        try {
            IToken tokenSold = (IToken) getTokens().get(soldId);
            IToken tokenBuy = (IToken) getTokens().get(buyId);

            BigInteger balanceBefore = tokenSold.balanceOf(getAddress());
            BigInteger balanceAfter = TokenMath.increaseBalance(tokenSold,
                                                                getAddress(),
                                                                amountSold);
            log.info("{} balance {} -> {}", tokenSold.getSymbol(), balanceBefore, balanceAfter);

            balanceBefore = tokenBuy.balanceOf(getAddress());
            balanceAfter = TokenMath.decreaseBalance(tokenBuy, getAddress(), amountBuy);
            log.info("{} balance {} -> {}", tokenBuy.getSymbol(), balanceBefore, balanceAfter);

            // It's easy when amountSold is equal to token received by contract
            balanceBefore = balances.get(soldId);
            balanceAfter = TokenMath.safeAdd(balanceBefore, amountSold);
            balances.set(soldId, balanceAfter);
            log.info("balance{} {} -> {}", soldId, balanceBefore, balanceAfter);

            balanceBefore = balances.get(buyId);
            balanceAfter = TokenMath.safeSubtract(balanceBefore,
                                                  TokenMath.safeAdd(amountBuy, dyAdminFee));
            balances.set(buyId, balanceAfter);
            log.info("balance{} {} -> {}", buyId, balanceBefore, balanceAfter);
        } finally {
            wlock.unlock();
        }
    }

    private void handleTokenExchangeUnderlyingEvent(EventValues eventValues, long eventTime) {
        log.info("handleTokenExchangeUnderlyingEvent {}", getAddress());
        int soldId = ((Int128) eventValues.getNonIndexedValues().get(0)).getValue().intValue();
        BigInteger amountSold = ((Uint256) eventValues.getNonIndexedValues().get(1)).getValue();
        int buyId = ((Int128) eventValues.getNonIndexedValues().get(2)).getValue().intValue();
        BigInteger amountBuy = ((Uint256) eventValues.getNonIndexedValues().get(3)).getValue();
        if (soldId != TOKEN_ID && buyId != TOKEN_ID) {
            // exchange in base pool
            return;
        }

        IToken tokenSold = (IToken) getTokens().get(soldId);
        IToken tokenBuy = (IToken) getTokens().get(buyId);

        int i = soldId == TOKEN_ID ? TOKEN_ID : LP_TOKEN_ID;
        int j = buyId == TOKEN_ID ? TOKEN_ID : LP_TOKEN_ID;

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
            wlock.lock();
            try {
                BigInteger balanceBefore = tokenSold.balanceOf(getAddress());
                BigInteger balanceAfter = TokenMath.increaseBalance(tokenSold,
                                                                    getAddress(),
                                                                    amountSold);
                log.info("{} balance {} -> {}", tokenSold.getSymbol(), balanceBefore, balanceAfter);

                balanceBefore = tokenBuy.balanceOf(getAddress());
                balanceAfter = TokenMath.decreaseBalance(tokenBuy, getAddress(), amountBuy);
                log.info("{} balance {} -> {}", tokenBuy.getSymbol(), balanceBefore, balanceAfter);

                // It's easy when amountSold is equal to token received by contract
                balanceBefore = balances.get(i);
                balanceAfter = TokenMath.safeAdd(balanceBefore, amountSold);
                balances.set(i, balanceAfter);
                log.info("balance{} {} -> {}", i, balanceBefore, balanceAfter);

                balanceBefore = balances.get(j);
                balanceAfter = TokenMath.safeSubtract(balanceBefore,
                                                      TokenMath.safeAdd(amountBuy, dyAdminFee));
                balances.set(j, balanceAfter);
                log.info("balance{} {} -> {}", j, balanceBefore, balanceAfter);
            } finally {
                wlock.unlock();
            }
            return;
        }
        // dyAdminFee =
        //   dy * fee / FEE_DENOMINATOR * adminFee / FEE_DENOMINATOR * PRECISION / RATES[j]
        BigInteger dy = dyAdminFee.multiply(FEE_DENOMINATOR)
                                  .multiply(FEE_DENOMINATOR)
                                  .multiply(RATES.get(j))
                                  .divide(fee)
                                  .divide(adminFee)
                                  .divide(PRECISION);
        // dy = xp[j] - y - 1
        BigInteger price = getVirtualPrice(eventTime, true);
        List<BigInteger> xp = getXP(price);
        BigInteger y = xp.get(j).subtract(dy).subtract(BigInteger.ONE);
        // use y cal x
        BigInteger A = getA(eventTime);
        BigInteger x = getY(buyId, soldId, y, xp, A);
        // x = dx * RATES[i] / PRECISION + xp[i] where RATES[i] = price
        BigInteger dx = x.subtract(xp.get(i)).multiply(PRECISION).divide(price);
        assert dx.compareTo(BigInteger.ZERO) >= 0;
        wlock.lock();
        try {
            BigInteger balanceBefore = tokenSold.balanceOf(getAddress());
            BigInteger balanceAfter = TokenMath.increaseBalance(tokenSold,
                                                                getAddress(),
                                                                amountSold);
            log.info("{} balance {} -> {}", tokenSold.getSymbol(), balanceBefore, balanceAfter);

            balanceBefore = tokenBuy.balanceOf(getAddress());
            balanceAfter = TokenMath.decreaseBalance(tokenBuy, getAddress(), amountBuy);
            log.info("{} balance {} -> {}", tokenBuy.getSymbol(), balanceBefore, balanceAfter);

            balanceBefore = balances.get(i);
            balanceAfter = TokenMath.safeAdd(balanceBefore, dx);
            balances.set(i, balanceAfter);
            log.info("balance{} {} -> {}", i, balanceBefore, balanceAfter);

            balanceBefore = balances.get(j);
            balanceAfter = TokenMath.safeSubtract(balanceBefore,
                                                  TokenMath.safeAdd(amountBuy, dyAdminFee));
            balances.set(j, balanceAfter);
            log.info("balance{} {} -> {}", j, balanceBefore, balanceAfter);
        } finally {
            wlock.unlock();
        }
    }

    private void initUnderlyingPool() {
        List<Type> response = abi.invoke(Curve4Abi.Functions.BASE_POOL, Collections.emptyList());
        String poolAddress
            = AddressConverter.EthToTronBase58Address(((Address) response.get(0)).getValue());
        underlyingPool = (CurvePool) contractManager.getContract(poolAddress);
        if (null == underlyingPool) {
            throw new RuntimeException(getType().name() +
                                       " " +
                                       getAddress() +
                                       " should initialize after " +
                                       poolAddress);
        }
    }
}
