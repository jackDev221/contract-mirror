package org.tron.sunio.contract_mirror.mirror.dao;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.tron.sunio.contract_mirror.event_decode.utils.GsonUtil;

@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
public class AssemblePoolData extends BaseContractData {
    private String pool;
    private String token;
    private String basePool;
    private String baseLp;
    private String[] coins;
    private String[] baseCoins;

    public void updateCoins(int index, String address) {
        if (index >= coins.length) {
            System.out.println("Out of range!!");
        }
        coins[index] = address;
    }

    public void updateBaseCoins(int index, String address) {
        if (index >= baseCoins.length) {
            System.out.println("Out of range!!");
        }
        baseCoins[index] = address;
    }

    public AssemblePoolData copySelf() {
        String jsonString = GsonUtil.objectToGson(this);
        return GsonUtil.gsonToObject(jsonString, AssemblePoolData.class);
    }
}
