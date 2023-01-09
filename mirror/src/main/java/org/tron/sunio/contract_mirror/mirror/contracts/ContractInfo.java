package org.tron.sunio.contract_mirror.mirror.contracts;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.tron.sunio.contract_mirror.mirror.enums.ContractType;

@Data
@AllArgsConstructor
public class ContractInfo {
    private String address;
    private ContractType contractType;
}
