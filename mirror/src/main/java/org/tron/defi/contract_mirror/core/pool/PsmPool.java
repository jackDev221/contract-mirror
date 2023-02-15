package org.tron.defi.contract_mirror.core.pool;

import lombok.extern.slf4j.Slf4j;
import org.tron.defi.contract_mirror.core.Contract;
import org.tron.defi.contract_mirror.core.token.TRC20;
import org.tron.defi.contract_mirror.core.token.Token;
import org.tron.defi.contract_mirror.dao.ContractLog;
import org.tron.defi.contract_mirror.dao.KafkaMessage;
import org.tron.defi.contract_mirror.utils.abi.AbiDecoder;

@Slf4j
public class PsmPool extends Pool {
    public PsmPool(String address) {
        super(address);
        type = PoolType.PSM;
    }

    @Override
    public boolean init() {
        try {
            tokens.add(getUsddFromChain());
            tokens.add(getGemFromChain());
            updateName();
        } catch (RuntimeException e) {
            log.error(e.getMessage());
            return false;
        }
        return true;
    }

    public Token getUsdd() {
        final int usddId = 0;
        return tokens.get(usddId);
    }

    public Token getGem() {
        final int gemId = 1;
        return tokens.get(gemId);
    }

    private Token getUsddFromChain() {
        final String SIGNATURE = "usdd()";
        String result = contractTrigger.triggerConstant(getAddress(), SIGNATURE);
        if (result.isEmpty()) {
            throw new RuntimeException();
        }
        String tokenAddress = AbiDecoder.DecodeAddress(result).left;
        Contract contract = contractManager.getContract(tokenAddress);
        return null != contract
               ? (Token) contract
               : (Token) contractManager.registerContract(new TRC20(tokenAddress));
    }

    private Token getGemFromChain() {
        final String GEM_JOIN_SIGNATURE = "gemJoin()";
        String result = contractTrigger.triggerConstant(getAddress(), GEM_JOIN_SIGNATURE);
        if (result.isEmpty()) {
            throw new RuntimeException();
        }
        String gemJoinAddress = AbiDecoder.DecodeAddress(result).left;
        final String GEM_SIGNATURE = "gem()";
        result = contractTrigger.triggerConstant(gemJoinAddress, GEM_SIGNATURE);
        if (result.isEmpty()) {
            throw new RuntimeException();
        }
        String tokenAddress = AbiDecoder.DecodeAddress(result).left;
        Contract contract = contractManager.getContract(tokenAddress);
        return null != contract
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
