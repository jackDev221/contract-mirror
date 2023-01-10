package org.tron.sunio.contract_mirror.event_decode;

import org.tron.sunio.contract_mirror.event_decode.logdata.ContractEventLog;
import org.tron.sunio.contract_mirror.event_decode.utils.GsonUtil;

public class LogDecode {
    public static ContractEventLog decode(String source) {
        return GsonUtil.gsonToObject(source, ContractEventLog.class);
    }
}
