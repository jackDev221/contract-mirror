package org.tron.defi.contract_mirror.core.pool;

import org.tron.defi.contract.abi.ContractAbi;
import org.tron.defi.contract.abi.pool.SunswapV2Abi;
import org.tron.defi.contract.log.ContractLog;
import org.tron.defi.contract_mirror.core.Contract;
import org.tron.defi.contract_mirror.core.token.TRC20;
import org.tron.defi.contract_mirror.core.token.Token;
import org.tron.defi.contract_mirror.dao.KafkaMessage;
import org.tron.defi.contract_mirror.utils.chain.AddressConverter;
import org.web3j.abi.datatypes.Address;
import org.web3j.abi.datatypes.Type;

import java.util.Collections;
import java.util.List;

public class SunswapV2Pool extends Pool {
    public SunswapV2Pool(String address) {
        super(address);
        this.type = PoolType.SUNSWAP_V2;
    }

    @Override
    protected ContractAbi loadAbi() {
        return tronContractTrigger.contractAt(SunswapV2Abi.class, getAddress());
    }

    @Override
    public boolean init() {
        return tokens.size() == 2;
    }

    public Token getToken0() {
        return getTokenFromChain(0);
    }

    public Token getToken1() {
        return getTokenFromChain(1);
    }

    private Token getTokenFromChain(int n) {
        if (tokens.size() > n) {
            return tokens.get(n);
        }
        if (n > 1) {
            throw new IllegalArgumentException("n = " + n);
        }
        List<Type> response = abi.invoke((n == 0
                                          ? SunswapV2Abi.Functions.TOKEN0
                                          : SunswapV2Abi.Functions.TOKEN1),
                                         Collections.emptyList());
        String tokenAddress
            = AddressConverter.EthToTronBase58Address(((Address) response.get(0)).getValue());
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
