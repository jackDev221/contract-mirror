package org.tron.sunio.contract_mirror.mirror.contracts.factory;

import lombok.extern.slf4j.Slf4j;
import org.tron.sunio.contract_mirror.event_decode.logdata.ContractEventLog;
import org.tron.sunio.contract_mirror.mirror.chainHelper.IChainHelper;
import org.tron.sunio.contract_mirror.mirror.contracts.BaseContract;
import org.tron.sunio.contract_mirror.mirror.contracts.IContractFactory;
import org.tron.sunio.contract_mirror.mirror.db.IDbHandler;
import org.tron.sunio.contract_mirror.mirror.enums.ContractType;

import java.util.List;
import java.util.Map;

import static org.tron.sunio.contract_mirror.event_decode.events.SwapV2FactoryEvent.EVENT_NAME_PAIR_CREATED_MINT;

@Slf4j
public class SwapFactoryV2 extends BaseContract implements IContractFactory {

    public SwapFactoryV2(String address, ContractType type, IChainHelper iChainHelper, IDbHandler iDbHandler,
                         Map<String, String> sigMap) {
        super(address, type, iChainHelper, iDbHandler, sigMap);
    }

    @Override
    public BaseContract getBaseContract() {
        return null;
    }

    @Override
    public List<BaseContract> getListContracts() {
        return null;
    }

    @Override
    public List<String> getListContractAddresses() {
        return null;
    }

    @Override
    public String getFactoryState() {
        return null;
    }

    @Override
    public void handleEvent(ContractEventLog contractEventLog) {
        super.handleEvent(contractEventLog);
        if (!isReady) {
            return;
        }
        // Do handleEvent
        String eventName = getEventName(contractEventLog);
        String[] topics = contractEventLog.getTopicList();
        switch (eventName) {
            case EVENT_NAME_PAIR_CREATED_MINT:
                handleCreatePair(topics, contractEventLog.getData());
                break;
            default:
                log.warn("event:{} not handle", topics[0]);
                break;
        }
    }

    private void handleCreatePair(String[] topics, String data) {
        log.info("handleCreatePair not implements!");
    }
}
