package org.tron.sunio.contract_mirror.mirror.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Objects;
import java.util.stream.Stream;

@Getter
@AllArgsConstructor
public enum ContractType {
    SWAP_V1("SWAP_V1"),
    SWAP_V2_PAIR("SWAP_V2_PAIR"),
    CONTRACT_CURVE_2POOL("CONTRACT_CURVE_2POOL"),
    CONTRACT_CURVE_3POOL("CONTRACT_CURVE_3POOL"),
    CONTRACT_CURVE_4POOL("CONTRACT_CURVE_4POOL"),
    CONTRACT_PSM("CONTRACT_PSM"),
    SWAP_FACTORY_V1("SWAP_FACTORY_V1"),
    SWAP_FACTORY_V2("SWAP_FACTORY_V2"),
    STABLE_SWAP_POOL("STABLE_SWAP_POOL");
    private final String desc;

    public static ContractType find(final String desc) {
        if (Objects.isNull(desc)) {
            return null;
        }
        return Stream.of(ContractType.values())
                .filter(p -> p.desc.equalsIgnoreCase(desc))
                .findFirst()
                .orElse(null);
    }
}
