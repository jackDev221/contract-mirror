package org.tron.sunio.contract_mirror.mirror.consts;

import java.math.BigInteger;

public interface ContractMirrorConst {
    boolean IS_DEBUG = false;
    String EMPTY_ADDRESS = "T9yD14Nj9j7xAB4dbGeiX9h8unkKHxuWwb";
    String EMPTY_TOPIC_VALUE = "0x0000000000000000000000000000000000000000";
    String NETWORK_NILE = "nile";
    String NETWORK_MAIN = "main";
    String KAFKA_TOPIC_CONTRACT_LOG = "contractlog";
    String KAFKA_TOPIC_CONTRACT_EVENT_LOG = "contractevent";
    String KAFKA_TOPIC_NILE_CONTRACT_LOG = "nile_contractlog";
    String KAFKA_TOPIC_NILE_CONTRACT_EVENT_LOG = "nile_contractevent";
    String CONTRACT_CONST_METHOD = "/contract/{address}/{method}";
    String CONTRACT_VERSION = "/version/{address}";
    String CONTRACT_ROUTING = "/routingInV2";
    BigInteger SWAP_V1_NO_FEE = BigInteger.TWO.pow(128).subtract(BigInteger.ONE);

    // version
    String V1_VERSION = "v1";
    String V1_FACTORY = "v1factory";
    String V2_VERSION = "v2";
    String V2_FACTORY = "v2factory";

    //Methods
    String METHOD_VERSION = "version";
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

    // 4Pool
    String METHOD_POOL = "pool";
    String METHOD_BASE_POOL = "base_pool";
    String METHOD_BASE_LP = "base_lp";
    //    String METHOD_TOKEN = "token";
    String METHOD_COINS = "coins";
    String METHOD_BASE_COINS = "base_coins";
    //PSM
    String METHOD_GEM_JOIN = "gemJoin";
    String METHOD_USDD = "usdd";
    String METHOD_USDDJOIN = "usddJoin";
    String METHOD_VAT = "vat";
    String METHOD_TIN = "tin";
    String METHOD_TOUT = "tout";
    String METHOD_QUOTA = "quota";

    String CALL_FOR_ROUTER = "call_for_router";
    String INIT_TX_ID = "0000000000000000000000000000000000000000000000000000000000000000";
}
