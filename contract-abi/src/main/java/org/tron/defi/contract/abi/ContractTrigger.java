package org.tron.defi.contract.abi;

public interface ContractTrigger {
    long balance(String address);

    ContractAbi contractAt(Class<? extends Contract> abi, String address);

    String trigger(String address, String functionSelector);

    String trigger(String address, String functionSelector, String params);
}
