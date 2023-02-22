package org.tron.sunio.contract_mirror.mirror.dao;

import lombok.Data;
import org.tron.sunio.contract_mirror.mirror.tools.DeepCopyUtils;

import java.math.BigInteger;

/***
 *
 * coins: public(address[N_COINS])
 * balances: public(uint256[N_COINS])
 * fee: public(uint256)  # fee * 1e10
 * admin_fee: public(uint256)  # admin_fee * 1e10
 * owner: public(address)
 * fee_converter: public(address)
 * lp_token: public(address)
 *
 * # Token corresponding to the pool is always the last one
 * BASE_CACHE_EXPIRES: constant(int128) = 10 * 60  # 10 min
 * base_pool: public(address)
 * base_virtual_price: public(uint256)
 * base_cache_updated: public(uint256)
 * base_coins: public(address[BASE_N_COINS])
 * base_lp: public(address)
 *
 * A_PRECISION: constant(uint256) = 100
 * initial_A: public(uint256)
 * future_A: public(uint256)
 * initial_A_time: public(uint256)
 * future_A_time: public(uint256)
 *
 * admin_actions_deadline: public(uint256)
 * transfer_ownership_deadline: public(uint256)
 * future_fee: public(uint256)
 * future_admin_fee: public(uint256)
 * future_owner: public(address)
 */

@Data
public class StableSwapPoolData extends BaseContractData {
    private String[] coins;
    private String[] coinNames;
    private String[] coinSymbols;
    private BigInteger[] balances;
    private BigInteger fee;
    private BigInteger adminFee;
    private String owner;
    private String feeConverter;
    private String lpToken;
    private String basePool;
    private BigInteger baseVirtualPrice;
    private BigInteger baseCacheUpdated;
    private String[] baseCoins;
    private String[] baseCoinNames;
    private String[] baseCoinSymbols;
    private String baseLp;
    private BigInteger initialA;
    private BigInteger futureA;
    private BigInteger initialATime;
    private BigInteger futureATime;
    private BigInteger adminActionsDeadline;
    private BigInteger transferOwnershipDeadline;
    private BigInteger futureFee;
    private BigInteger futureAdminFee;
    private String futureOwner;
    private BigInteger lpTotalSupply;
    private BigInteger baseLpTotalSupply;

    public StableSwapPoolData(int coinsCount, int baseCoinsCount) {
        coins = new String[coinsCount];
        coinNames = new String[coinsCount];
        coinSymbols = new String[coinsCount];
        balances = new BigInteger[coinsCount];
        baseCoins = new String[baseCoinsCount];
        baseCoinNames = new String[baseCoinsCount];
        baseCoinSymbols = new String[baseCoinsCount];
    }

    public BigInteger[] getCopyBalances() {
        BigInteger[] result = new BigInteger[balances.length];
        for (int i = 0; i < balances.length; i++) {
            result[i] = new BigInteger(balances[i].toString());
        }
        return result;
    }

    public void updateBalances(int index, BigInteger value) {
        if (index >= balances.length) {
            System.out.println("Out of range!!");
        }
        balances[index] = value;
    }

    public StableSwapPoolData copySelf() {
        return DeepCopyUtils.deepCopy(this, StableSwapPoolData.class);
    }
}
