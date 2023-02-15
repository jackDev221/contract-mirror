package org.tron.defi.contract_mirror.core.token;

import org.tron.defi.contract_mirror.common.ContractType;

public class TRX extends Token {
    private static final String TRX_ADDRESS = "T9yD14Nj9j7xAB4dbGeiX9h8unkKHxuWwb";
    private static final String TRX_SYMBOL = "TRX";
    private static final int TRX_DECIMALS = 6;
    private static final TRX instance = new TRX();

    TRX() {
        super(TRX_ADDRESS);
        symbol = TRX_SYMBOL;
        decimals = TRX_DECIMALS;
    }

    public static TRX getInstance() {
        return instance;
    }

    @Override
    public String getContractType() {
        return ContractType.TRX_TOKEN.name();
    }
}
