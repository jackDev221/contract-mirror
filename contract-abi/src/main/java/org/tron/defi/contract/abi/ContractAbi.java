package org.tron.defi.contract.abi;

import org.tron.defi.contract.log.ContractLog;
import org.web3j.abi.EventValues;
import org.web3j.abi.datatypes.Type;

import java.util.List;

public interface ContractAbi {
    EventValues decodeEvent(ContractLog message);

    EventPrototype getEvent(String signature);

    FunctionPrototype getFunction(String signature);

    List<Type> invoke(String address, IFunction function, List<Object> params);

    List<Type> invoke(IFunction function, List<Object> params);
}
