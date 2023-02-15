package org.tron.defi.contract_mirror.core.token;

import org.tron.defi.contract_mirror.common.ContractType;
import org.tron.defi.contract_mirror.utils.abi.AbiDecoder;

public class TRC20 extends Token {
    public TRC20(String address) {
        super(address);
    }

    @Override
    public String getContractType() {
        return ContractType.TRC20_TOKEN.name();
    }

    @Override
    public String getSymbol() {
        if (null != symbol) {
            return symbol;
        }
        return getSymbolFromChain();
    }

    @Override
    public int getDecimals() {
        if (0 != decimals) {
            return decimals;
        }
        return getDecimalsFromChain();
    }

    private String getSymbolFromChain() {
        final String SIGNATURE = "symbol()";
        String result = contractTrigger.triggerConstant(getAddress(), SIGNATURE);
        if (result.isEmpty()) {
            throw new RuntimeException();
        }
        symbol = AbiDecoder.DecodeStringFromTuple(result, 0);
        return symbol;
    }

    private int getDecimalsFromChain() {
        final String SIGNATURE = "decimals()";
        String result = contractTrigger.triggerConstant(getAddress(), SIGNATURE);
        if (result.isEmpty()) {
            throw new RuntimeException();
        }
        decimals = AbiDecoder.DecodeNumber(result).left.intValue();
        return decimals;
    }
}
