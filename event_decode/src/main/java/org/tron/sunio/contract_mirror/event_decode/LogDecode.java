package org.tron.sunio.contract_mirror.event_decode;

import org.tron.sunio.contract_mirror.event_decode.logdata.BlockLog;
import org.tron.sunio.contract_mirror.event_decode.logdata.ContractEventLog;
import org.tron.sunio.contract_mirror.event_decode.logdata.ContractLog;
import org.tron.sunio.contract_mirror.event_decode.logdata.TransactionLog;
import org.tron.sunio.contract_mirror.event_decode.utils.GsonUtil;

public class LogDecode {
    public static ContractLog decodeContractLog(String source) {
        return GsonUtil.gsonToObject(source, ContractLog.class);
    }

    public static ContractEventLog decodeContractEventLog(String source) {
        return GsonUtil.gsonToObject(source, ContractEventLog.class);
    }

    public static TransactionLog decodeTransactionLog(String source) {
        return GsonUtil.gsonToObject(source, TransactionLog.class);
    }

    public static BlockLog decodeBlockLog(String source) {
        return GsonUtil.gsonToObject(source, BlockLog.class);
    }

}
