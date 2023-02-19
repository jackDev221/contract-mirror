package org.tron.defi.contract.abi;

public interface ContractTrigger {
    ContractAbi contractAt(Class<? extends Contract> abi, String address);

    long balance(String address);

    String trigger(String address, String functionSelector);

    String trigger(String address, String functionSelector, String params);
}
