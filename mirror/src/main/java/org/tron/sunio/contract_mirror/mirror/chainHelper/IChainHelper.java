package org.tron.sunio.contract_mirror.mirror.chainHelper;

import org.web3j.abi.TypeReference;
import org.web3j.abi.datatypes.Type;

import java.math.BigInteger;
import java.util.List;

public interface IChainHelper {
    long blockNumber();

    BigInteger balance(String address);

    List<Type> triggerConstantContract(TriggerContractInfo triggerContractInfo);
}
