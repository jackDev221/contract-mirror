package org.tron.defi.contract.abi;

import org.tron.defi.contract.log.ContractLog;
import org.web3j.abi.EventValues;
import org.web3j.abi.datatypes.Type;

import java.util.List;

public interface ContractAbi {
    List<Type> invoke(IFunction function, List<Object> params);

    List<Type> invoke(String address, IFunction function, List<Object> params);

    EventValues decodeEvent(ContractLog message);

    FunctionPrototype getFunction(String signature);

    EventPrototype getEvent(String signature);
}
