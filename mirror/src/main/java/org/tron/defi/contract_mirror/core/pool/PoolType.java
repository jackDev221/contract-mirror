package org.tron.defi.contract_mirror.core.pool;

import org.tron.defi.contract_mirror.core.ContractType;

public enum PoolType {
    UNKNOWN,
    SUNSWAP_V1,
    SUNSWAP_V2,
    CURVE,
    CURVE_COMBINATION,
    PSM,
    WTRX;

    public static PoolType convertFromContractType(ContractType type) {
        switch (type) {
            case CURVE_POOL:
                return CURVE;
            case CURVE_COMBINATION_POOL:
                return CURVE_COMBINATION;
            case PSM_POOL:
                return PSM;
            default:
                throw new IllegalArgumentException();
        }
    }
}
