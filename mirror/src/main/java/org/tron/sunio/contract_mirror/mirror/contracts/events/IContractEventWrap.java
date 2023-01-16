package org.tron.sunio.contract_mirror.mirror.contracts.events;

public interface IContractEventWrap {
    long getTimeStamp();

    String[] getTopics();

    String getData();

    String getUniqueId();

    String getBlockHash();

    long getBlockNumber();

    String getContractAddress();
}
