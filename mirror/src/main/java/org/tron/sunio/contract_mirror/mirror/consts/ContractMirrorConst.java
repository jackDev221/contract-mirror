package org.tron.sunio.contract_mirror.mirror.consts;

public interface ContractMirrorConst {
    String EMPTY_ADDRESS = "T9yD14Nj9j7xAB4dbGeiX9h8unkKHxuWwb";
    String EMPTY_TOPIC_VALUE = "0000000000000000000000000000000000000000000000000000000000000000";
    String KAFKA_TOPIC_CONTRACT_LOG = "contractlog";
    String KAFKA_TOPIC_CONTRACT_EVENT_LOG = "contractevent";
    String CONTRACT_CONST_METHOD = "/contract/{address}/{method}";

    //Methods
    String METHOD_STATUS = "status";
    //SwapV1Factory
    String METHOD_GET_EXCHANGE = "getExchange";
    String METHOD_GET_TOKEN = "getToken";
    String METHOD_GET_TOKEN_WITH_ID = "getTokenWithId";
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
    //SwapV2Factory
    String METHOD_FEE_TO_SETTER = "feeToSetter";
    String METHOD_ALL_PAIRS_LENGTH = "allPairsLength";
    String METHOD_GET_PAIR = "getPair";
    String METHOD_ALL_PAIRS = "allPairs";
    //SwapV2Pair
    String METHOD_FACTORY = "factory";
    String METHOD_TOKEN0 = "token0";
    String METHOD_TOKEN1 = "token1";
    String METHOD_PRICE0_CUMULATIVE_LAST = "price0CumulativeLast";
    String METHOD_PRICE1_CUMULATIVE_LAST = "price1CumulativeLast";
    String METHOD_GET_RESERVES = "getReserves";
    //CurvePool
    String METHOD_FEE = "fee";
    String METHOD_FUTURE_FEE = "future_fee";
    String METHOD_ADMIN_FEE = "admin_fee";
    String METHOD_FUTURE_ADMIN_FEE = "future_admin_fee";
    String METHOD_ADMIN_ACTIONS_DEADLINE = "admin_actions_deadline";
    String METHOD_FEE_CONVERTER = "fee_converter";
    String METHOD_INITIAL_A = "initial_A";
    String METHOD_INITIAL_A_TIME = "initial_A_time";
    String METHOD_FUTURE_A = "future_A";
    String METHOD_FUTURE_A_TIME = "future_A_time";
    String METHOD_OWNER = "owner";
    String METHOD_FUTURE_OWNER = "future_owner";
    String METHOD_TRANSFER_OWNERSHIP_DEADLINE = "transfer_ownership_deadline";


}
