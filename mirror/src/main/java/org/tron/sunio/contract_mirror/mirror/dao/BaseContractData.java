package org.tron.sunio.contract_mirror.mirror.dao;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.tron.sunio.contract_mirror.mirror.contracts.ContractObj;
import org.tron.sunio.contract_mirror.mirror.enums.ContractType;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BaseContractData {
    protected String address;
    protected ContractType type;
    protected String version;
    protected ContractObj.ContractStateInfo stateInfo;
}
