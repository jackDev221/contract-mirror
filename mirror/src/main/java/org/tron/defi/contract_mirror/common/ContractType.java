package org.tron.defi.contract_mirror.common;

public enum ContractType {
    UNKNOWN,
    // factories
    SUNSWAP_FACTORY_V1,
    SUNSWAP_FACTORY_V2,
    // pools
    CURVE_2POOL,
    CURVE_3POOL,
    CURVE_COMBINATION_4POOL,
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
