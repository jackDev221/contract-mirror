package org.tron.sunio.contract_mirror.mirror.consts;

public interface ContractMirrorConst {
    String EMPTY_ADDRESS = "T9yD14Nj9j7xAB4dbGeiX9h8unkKHxuWwb";
    String EMPTY_TOPIC_VALUE = "0000000000000000000000000000000000000000000000000000000000000000";
    String KAFKA_TOPIC_CONTRACT_LOG = "contractlog";
    String KAFKA_TOPIC_CONTRACT_EVENT_LOG = "contractevent";
    String CONTRACT_CONST_METHOD = "/contract/{address}/const/{method}";

    //Methods
    String METHOD_STATUS = "status";
    //SwapV1Factory
    String METHOD_FEE_TO = "feeTo";
    String METHOD_FEE_TO_RATE = "feeToRate";
    String METHOD_TOKEN_COUNT = "tokenCount";
    //SwapV1
    String METHOD_NAME = "name";
    String METHOD_TOKEN = "token";
    String METHOD_DECIMALS = "decimals";
    String METHOD_SYMBOL = "symbol";
    String METHOD_K_LAST = "kLast";
    String METHOD_TOTAL_SUPPLY = "totalSupply";
    String METHOD_BALANCE = "balance";
    String METHOD_TONE_BALANCE = "tokenBalance";


}
