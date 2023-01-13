package org.tron.sunio.contract_mirror.mirror.contracts;

public interface IContractsCollectHelper {
    void addContract(BaseContract baseContract);

    BaseContract getContract(String address);

    boolean containsContract(String address);
}
