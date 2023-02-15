package org.tron.defi.contract_mirror.core.pool;

import org.tron.defi.contract_mirror.core.Contract;
import org.tron.defi.contract_mirror.core.token.TRC20;
import org.tron.defi.contract_mirror.core.token.Token;
import org.tron.defi.contract_mirror.dao.ContractLog;
import org.tron.defi.contract_mirror.dao.KafkaMessage;
import org.tron.defi.contract_mirror.utils.abi.AbiDecoder;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class SunswapV2Pool extends Pool {
    private final List<String> TOKEN_METHOD = new ArrayList<>(Arrays.asList("token0()",
                                                                            "token1()"));

    public SunswapV2Pool(String address) {
        super(address);
        this.type = PoolType.SUNSWAP_V2;
    }

    @Override
    public boolean init() {
        return tokens.size() == 2;
    }

    public Token getToken0() {
        return getToken(0);
    }

    public Token getToken1() {

        return getToken(1);
    }

    private Token getToken(int n) {
        if (tokens.size() > n) {
            return tokens.get(n);
        }
        String result = contractTrigger.triggerConstant(getAddress(), TOKEN_METHOD.get(n));
        if (result.isEmpty()) {
            return null;
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
