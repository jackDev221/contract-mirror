package org.tron.defi.contract_mirror.core.pool;

import com.alibaba.fastjson.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.tron.defi.contract_mirror.core.Contract;
import org.tron.defi.contract_mirror.core.token.TRC20;
import org.tron.defi.contract_mirror.core.token.Token;
import org.tron.defi.contract_mirror.dao.ContractLog;
import org.tron.defi.contract_mirror.dao.KafkaMessage;
import org.tron.defi.contract_mirror.utils.abi.AbiDecoder;

@Slf4j
public class Curve4Pool extends Pool {
    private CurvePool underlyingPool;

    public Curve4Pool(String address) {
        super(address);
        type = PoolType.CURVE4;
    }

    @Override
    public boolean init() {
        try {
            initUnderlyingPool();
            tokens.addAll(underlyingPool.getTokens());
            tokens.add(getTokenFromChain(0));
            Token underlyingLpToken = getTokenFromChain(1);
            if (!underlyingLpToken.equals(underlyingPool.getLpToken())) {
                throw new IllegalArgumentException("UNDERLYING LP_TOKEN MISMATCH");
            }
            lpToken = getLpTokenFromChain();
            updateName();
        } catch (RuntimeException e) {
            log.error(e.getMessage());
            return false;
        }
        return true;
    }

    @Override
    protected JSONObject getInfo() {
        JSONObject info = super.getInfo();
        info.put("base_pool", underlyingPool.getInfo());
        return info;
    }

    private void initUnderlyingPool() throws RuntimeException {
        final String BASE_POOL_SIGNATURE = "base_pool()";
        String result = contractTrigger.triggerConstant(getAddress(), BASE_POOL_SIGNATURE);
        if (result.isEmpty()) {
            throw new RuntimeException();
        }
        String poolAddress = AbiDecoder.DecodeAddress(result).left;
        underlyingPool = (CurvePool) contractManager.getContract(poolAddress);
        if (null == underlyingPool) {
            throw new RuntimeException(getType().name() +
                                       " " +
                                       getAddress() +
                                       " should initialize after " +
                                       poolAddress);
        }
    }

    private Token getTokenFromChain(int n) throws RuntimeException {
        if (n > 1) {
            throw new IndexOutOfBoundsException("n = " + n);
        }
        int i = underlyingPool.getN() + n;
        if (tokens.size() > i) {
            return tokens.get(i);
        }
        final String SIGNATURE = "coins(uint256)";
        String result = contractTrigger.triggerConstant(getAddress(), SIGNATURE, String.valueOf(n));
        if (result.isEmpty()) {
            throw new RuntimeException();
        }
        String tokenAddress = AbiDecoder.DecodeAddress(result).left;
        Contract contract = contractManager.getContract(tokenAddress);
        return contract != null
               ? (Token) contract
               : (Token) contractManager.registerContract(new TRC20(tokenAddress));
    }

    private Token getLpTokenFromChain() {
        final String SIGNATURE = "lp_token()";
        String result = contractTrigger.triggerConstant(getAddress(), SIGNATURE);
        if (result.isEmpty()) {
            throw new RuntimeException();
        }
        String tokenAddress = AbiDecoder.DecodeAddress(result).left;
        Contract contract = contractManager.getContract(tokenAddress);
        return contract != null
               ? (Token) contract
               : (Token) contractManager.registerContract(new TRC20(tokenAddress));
    }

    @Override
    public void sync() {
        // TODO finish implementation
    }

    @Override
    public void onEvent(KafkaMessage<ContractLog> kafkaMessage) {
        // TODO finish implementation
    }
}
