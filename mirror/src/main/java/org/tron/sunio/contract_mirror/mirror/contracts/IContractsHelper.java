package org.tron.sunio.contract_mirror.mirror.contracts;

public interface IContractsHelper {
    long getBlockTime();

    BaseContract getContract(String address);
}
