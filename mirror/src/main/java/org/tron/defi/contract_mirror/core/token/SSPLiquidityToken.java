package org.tron.defi.contract_mirror.core.token;

import lombok.Setter;
import org.tron.defi.contract.abi.ContractAbi;
import org.tron.defi.contract.abi.token.SSPLiquidityTokenAbi;
import org.tron.defi.contract_mirror.common.ContractType;
import org.web3j.abi.datatypes.Type;
import org.web3j.abi.datatypes.generated.Uint256;

import java.math.BigInteger;
import java.util.Collections;
import java.util.List;

public class SSPLiquidityToken extends TRC20 implements ISSPLP {
    @Setter
    private BigInteger totalSupply;

    public SSPLiquidityToken(String address) {
        super(address);
    }

    @Override
    public String getContractType() {
        return ContractType.SSP_LP_TOKEN.name();
    }

    @Override
    protected ContractAbi loadAbi() {
        return tronContractTrigger.contractAt(SSPLiquidityTokenAbi.class, getAddress());
    }

    @Override
    public BigInteger totalSupply() {
        if (null != totalSupply) {
            return totalSupply;
        }
        return getTotalSupplyFromChain();
    }

    public BigInteger getTotalSupplyFromChain() {
        List<Type> response = abi.invoke(SSPLiquidityTokenAbi.Functions.TOTAL_SUPPLY,
                                         Collections.emptyList());
        return ((Uint256) response.get(0)).getValue();
    }
}
