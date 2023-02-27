package org.tron.defi.contract_mirror.core.pool;

import org.tron.defi.contract_mirror.common.ContractType;

public enum PoolType {
    UNKNOWN,
    SUNSWAP_V1,
    SUNSWAP_V2,
    CURVE2,
    CURVE3,
    CURVE4,
    PSM,
    PSM_POLY;

    public static PoolType convertFromContractType(ContractType type) {
        switch (type) {
            case CURVE_2POOL:
                return CURVE2;
            case CURVE_3POOL:
                return CURVE3;
            case CURVE_4POOL:
                return CURVE4;
            case PSM_POOL:
                return PSM;
            default:
                throw new IllegalArgumentException();
        }
    }
}
