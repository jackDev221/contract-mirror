package org.tron.defi.contract_mirror.core.pool;

import lombok.extern.slf4j.Slf4j;
import org.tron.defi.contract.abi.ContractAbi;
import org.tron.defi.contract.abi.pool.SunswapV2Abi;
import org.tron.defi.contract_mirror.core.Contract;
import org.tron.defi.contract_mirror.core.token.TRC20;
import org.tron.defi.contract_mirror.core.token.Token;
import org.tron.defi.contract_mirror.utils.chain.AddressConverter;
import org.web3j.abi.EventValues;
import org.web3j.abi.datatypes.Address;
import org.web3j.abi.datatypes.Type;
import org.web3j.abi.datatypes.generated.Uint256;

import java.math.BigInteger;
import java.util.Collections;
import java.util.List;

@Slf4j
public class SunswapV2Pool extends Pool {
    public SunswapV2Pool(String address) {
        super(address);
        this.type = PoolType.SUNSWAP_V2;
    }

    @Override
    public void init() {
        sync();
    }

    @Override
    protected void getContractData() {
        try {
            TRC20 token0 = (TRC20) getTokens().get(0);
            TRC20 token1 = (TRC20) getTokens().get(1);
            token0.setBalance(getAddress(), token0.balanceOfFromChain(getAddress()));
            token1.setBalance(getAddress(), token1.balanceOfFromChain(getAddress()));
        } catch (Exception e) {
            log.error(e.getMessage());
        }
    }

    @Override
    protected void handleEvent(String eventName, EventValues eventValues, long eventTime) {
        if (eventName.equals("Sync")) {
            handleSyncEvent(eventValues);
        } else {
            // do nothing
        }
    }

    @Override
    protected ContractAbi loadAbi() {
        return tronContractTrigger.contractAt(SunswapV2Abi.class, getAddress());
    }

    public Token getToken0() {
        if (tokens.size() > 0) {
            return tokens.get(0);
        }
        return getTokenFromChain(0);
    }

    public Token getToken1() {
        if (tokens.size() > 1) {
            return tokens.get(1);
        }
        return getTokenFromChain(1);
    }

    private Token getTokenFromChain(int n) {
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

    private void handleSyncEvent(EventValues eventValues) {
        BigInteger balance0 = ((Uint256) eventValues.getNonIndexedValues().get(0)).getValue();
        BigInteger balance1 = ((Uint256) eventValues.getNonIndexedValues().get(1)).getValue();
        ((TRC20) getTokens().get(0)).setBalance(getAddress(), balance0);
        ((TRC20) getTokens().get(1)).setBalance(getAddress(), balance1);
    }
}
