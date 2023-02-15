package org.tron.defi.contract_mirror.core.pool;

import lombok.extern.slf4j.Slf4j;
import org.tron.defi.contract_mirror.core.Contract;
import org.tron.defi.contract_mirror.core.token.TRC20;
import org.tron.defi.contract_mirror.core.token.Token;
import org.tron.defi.contract_mirror.dao.ContractLog;
import org.tron.defi.contract_mirror.dao.KafkaMessage;
import org.tron.defi.contract_mirror.utils.abi.AbiDecoder;

import java.util.ArrayList;
import java.util.Arrays;

@Slf4j
public class CurvePool extends Pool {
    private static final ArrayList<PoolType> CURVE_TYPE
        = new ArrayList<>(Arrays.asList(PoolType.CURVE2, PoolType.CURVE3));

    public CurvePool(String address, PoolType type) {
        super(address);
        int index = CURVE_TYPE.indexOf(type);
        if (index < 0) {
            throw new IllegalArgumentException("Unexpected type: " + type.name());
        }
        this.type = type;
    }

    @Override
    public boolean init() {
        try {
            int n = getN();
            tokens.ensureCapacity(n);
            for (int i = 0; i < n; i++) {
                tokens.add(getTokenFromChain(i));
            }
            lpToken = getLpTokenFromChain();
            updateName();
        } catch (RuntimeException e) {
            log.error(e.getMessage());
            return false;
        }
        return true;
    }

    public int getN() {
        final int CURVE_TOKENS_MIN = 2;
        return tokens.size() > 0 ? tokens.size() : CURVE_TOKENS_MIN + CURVE_TYPE.indexOf(type);
    }

    private Token getTokenFromChain(int n) {
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
        final String SIGNATURE = "token()";
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
