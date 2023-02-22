package org.tron.defi.contract_mirror.core.pool;


import lombok.extern.slf4j.Slf4j;
import org.tron.defi.contract.abi.ContractAbi;
import org.tron.defi.contract.abi.pool.SunswapV1Abi;
import org.tron.defi.contract_mirror.core.token.TRC20;
import org.tron.defi.contract_mirror.core.token.TRX;
import org.web3j.abi.EventValues;
import org.web3j.abi.datatypes.generated.Uint256;

import java.math.BigInteger;

@Slf4j
public class SunswapV1Pool extends Pool {
    public SunswapV1Pool(String address) {
        super(address);
        this.type = PoolType.SUNSWAP_V1;
    }

    @Override
    public void init() {
        sync();
    }

    @Override
    protected void getContractData() {
        try {
            TRX trx = (TRX) getTokens().get(0);
            TRC20 token = (TRC20) getTokens().get(1);
            trx.setBalance(getAddress(), trx.balanceOfFromChain(getAddress()));
            token.setBalance(getAddress(), token.balanceOfFromChain(getAddress()));
        } catch (Exception e) {
            log.error(e.getMessage());
        }
    }

    @Override
    protected void handleEvent(String eventName, EventValues eventValues, long eventTime) {
        if (eventName.equals("Snapshot")) {
            handleSnapshotEvent(eventValues);
        }
    }

    @Override
    protected ContractAbi loadAbi() {
        return tronContractTrigger.contractAt(SunswapV1Abi.class, getAddress());
    }

    private void handleSnapshotEvent(EventValues eventValues) {
        BigInteger trxBalance = ((Uint256) eventValues.getIndexedValues().get(1)).getValue();
        BigInteger tokenBalance = ((Uint256) eventValues.getNonIndexedValues().get(2)).getValue();
        ((TRX) getTokens().get(0)).setBalance(getAddress(), trxBalance);
        ((TRC20) getTokens().get(1)).setBalance(getAddress(), tokenBalance);
    }
}
