package org.tron.defi.contract_mirror.core.pool;

import com.alibaba.fastjson.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.tron.defi.contract.abi.ContractAbi;
import org.tron.defi.contract.abi.pool.Curve4Abi;
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

@Slf4j
public class Curve4Pool extends Pool {
    private CurvePool underlyingPool;

    public Curve4Pool(String address) {
        super(address);
        type = PoolType.CURVE4;
    }

    @Override
    protected ContractAbi loadAbi() {
        return tronContractTrigger.contractAt(Curve4Abi.class, getAddress());
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

    private Token getLpTokenFromChain() {
        List<Type> response = abi.invoke(Curve4Abi.Functions.LP_TOKEN, Collections.emptyList());
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
