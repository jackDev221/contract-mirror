package org.tron.sunio.contract_mirror.mirror.contracts;

import lombok.Data;
import org.tron.sunio.contract_mirror.mirror.enums.ContractType;

@Data
public class ContractInfo {
    private String address;
    private ContractType type;
    private String extra;
}
