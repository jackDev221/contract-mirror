package org.tron.sunio.contract_mirror.mirror.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Objects;
import java.util.stream.Stream;

@Getter
@AllArgsConstructor
public enum ContractType {
    CONTRACT_V1("CONTRACT_V1"),
    CONTRACT_V2("CONTRACT_V2"),
    CONTRACT_SSP("CONTRACT_SSP"),
    CONTRACT_ROOTER("CONTRACT_ROOTER"),
    CONTRACT_FACTORY_V1("CONTRACT_FACTORY_V1");
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
