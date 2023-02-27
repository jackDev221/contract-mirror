package org.tron.sunio.contract_mirror.mirror.dao;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.tron.sunio.contract_mirror.mirror.tools.DeepCopyUtils;

import java.math.BigInteger;

@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
public class PSMData extends BaseContractData {
//    private String tokenName;
    private String polyAddress;
    private String gemJoin;
    private String usdd;
    private String usddJoin;
    private String vat;
    private BigInteger tin;
    private BigInteger tout;
    private String quota;
    private boolean enable; // info[7] == 1
    private BigInteger maxReversSwap;//info[0]
    private boolean reverseLimitEnable; // info[8] == 1
    private BigInteger swappedUSDD; // infos[1], USDT 已兑换 USDD 数量
    private BigInteger totalSwappedUSDD; // infos[6], Total 已兑换（固定18位）
    private BigInteger totalMaxSwapUSDD; // info[2]
    private BigInteger maxSwapUSDD; // info[3]
    private BigInteger usddBalance; // info[5]
    private BigInteger usdBalance; //   info[4]

    public PSMData copySelf() {
        return DeepCopyUtils.deepCopy(this, PSMData.class);
    }
}
