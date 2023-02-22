package org.tron.defi.contract_mirror.core.pool;

import com.alibaba.fastjson.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.tron.defi.contract.abi.ContractAbi;
import org.tron.defi.contract.abi.pool.Curve4Abi;
import org.tron.defi.contract.abi.pool.CurveAbi;
import org.tron.defi.contract_mirror.core.Contract;
import org.tron.defi.contract_mirror.core.token.SSPLiquidityToken;
import org.tron.defi.contract_mirror.core.token.TRC20;
import org.tron.defi.contract_mirror.core.token.Token;
import org.tron.defi.contract_mirror.utils.chain.AddressConverter;
import org.web3j.abi.EventValues;
import org.web3j.abi.datatypes.Address;
import org.web3j.abi.datatypes.Type;
import org.web3j.abi.datatypes.generated.Uint256;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@Slf4j
public class Curve4Pool extends Pool {
    private static final int N_COINS = 2;
    private static final BigInteger FEE_DENOMINATOR = new BigInteger(String.valueOf(Math.exp(10)));
    private static final BigInteger PRECISION = new BigInteger(String.valueOf(Math.exp(18)));
    private static final List<BigInteger> RATES = Arrays.asList(new BigInteger(
        "1000000000000000000000000000000"), new BigInteger("1000000000000000000"));
    private List<BigInteger> balances;
    private BigInteger fee;
    private BigInteger adminFee;
    private BigInteger initialA;
    private long timeInitialA;
    private BigInteger futureA;
    private long timeFutureA;
    private CurvePool underlyingPool;

    public Curve4Pool(String address) {
        super(address);
        type = PoolType.CURVE4;
    }

    @Override
    protected JSONObject getInfo() {
        JSONObject info = super.getInfo();
        info.put("base_pool", underlyingPool.getInfo());
        return info;
    }

    @Override
    public void init() {
        initUnderlyingPool();
        tokens.addAll(underlyingPool.getTokens());
        tokens.add(getTokenFromChain(0));
        Token underlyingLpToken = getTokenFromChain(1);
        if (!underlyingLpToken.equals(underlyingPool.getLpToken())) {
            throw new IllegalArgumentException("UNDERLYING LP_TOKEN MISMATCH");
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

        SSPLiquidityToken sspLpToklen = (SSPLiquidityToken) getLpToken();
        sspLpToklen.setTotalSupply(sspLpToklen.getTotalSupplyFromChain());

        for (int i = 0; i < N_COINS; i++) {
            response = abi.invoke(CurveAbi.Functions.BALANCES, Collections.singletonList(i));
            balances.set(i, ((Uint256) response.get(i)).getValue());

            TRC20 token = (TRC20) getTokens().get(i);
            token.setBalance(getAddress(), token.balanceOfFromChain(getAddress()));
        }
    }

    @Override
    protected void handleEvent(String eventName, EventValues eventValues, long eventTime) {

    }

    @Override
    protected ContractAbi loadAbi() {
        return tronContractTrigger.contractAt(Curve4Abi.class, getAddress());
    }

    private Token getLpTokenFromChain() {
        List<Type> response = abi.invoke(Curve4Abi.Functions.LP_TOKEN, Collections.emptyList());
        String tokenAddress
            = AddressConverter.EthToTronBase58Address(((Address) response.get(0)).getValue());
        Contract contract = contractManager.getContract(tokenAddress);
        return contract != null
               ? (Token) contract
               : (Token) contractManager.registerContract(new SSPLiquidityToken(tokenAddress));
    }

    private Token getTokenFromChain(int n) throws RuntimeException {
        if (n > 1) {
            throw new IndexOutOfBoundsException("n = " + n);
        }
        List<Type> response = abi.invoke(Curve4Abi.Functions.COINS, Collections.singletonList(n));
        String tokenAddress
            = AddressConverter.EthToTronBase58Address(((Address) response.get(0)).getValue());
        Contract contract = contractManager.getContract(tokenAddress);
        return contract != null
               ? (Token) contract
               : (Token) contractManager.registerContract(new TRC20(tokenAddress));
    }

    private void initUnderlyingPool() throws RuntimeException {
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
