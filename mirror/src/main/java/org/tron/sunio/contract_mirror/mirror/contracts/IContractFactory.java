package org.tron.sunio.contract_mirror.mirror.contracts;

import org.tron.sunio.contract_mirror.mirror.contracts.factory.BaseFactory;
import org.tron.sunio.contract_mirror.mirror.pool.CMPool;

import java.util.List;

public interface IContractFactory {
    BaseFactory getBaseContract();

    List<BaseContract> getListContracts(CMPool cmPool);
}
