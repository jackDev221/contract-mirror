package org.tron.sunio.contract_mirror.mirror.consts;

public interface ContractMirrorConst {
    String EMPTY_ADDRESS = "T9yD14Nj9j7xAB4dbGeiX9h8unkKHxuWwb";
    String EMPTY_TOPIC_VALUE = "0000000000000000000000000000000000000000000000000000000000000000";
    String KAFKA_TOPIC_CONTRACT_LOG = "contract log";
    String KAFKA_TOPIC_CONTRACT_EVENT_LOG = "event log";

    // url path
    String SWAP_V1_EX_STATUS = "/swapv1/exchange/{address}/status";
    String SWAP_V1_FAC_STATUS = "/swapv1/factory/{address}/status";
    String SWAP_V2_PAIR_STATUS = "/swapv2/pair/{address}/status";
    String SWAP_V2_FAC_STATUS = "/swapv2/factory/{address}/status";

    String CURVE_2POOL_STATUS = "/curve/2pool/{address}/status";
    String CURVE_3POOL_STATUS = "/curve/3pool/{address}/status";
    String PSM_STATUS = "/psm/{address}/status";

}
