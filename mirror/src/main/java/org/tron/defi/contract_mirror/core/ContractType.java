package org.tron.defi.contract_mirror.core;

public enum ContractType {
    UNKNOWN,
    // factories
    SUNSWAP_FACTORY_V1,
    SUNSWAP_FACTORY_V2,
    // pools
    CURVE_POOL,
    CURVE_COMBINATION_POOL,
    PSM_POOL,
    SUNSWAP_V1_POOL,
    SUNSWAP_V2_POOL,
    // tokens
    TRX_TOKEN,
    TRC20_TOKEN,
    WTRX_TOKEN,
    // poly
    PSM_POLY;

    public static ContractType contractType(String name) {
        for (ContractType value : ContractType.values()) {
            if (value.name().equals(name)) {
                return value;
            }
        }
        return UNKNOWN;
    }
}
