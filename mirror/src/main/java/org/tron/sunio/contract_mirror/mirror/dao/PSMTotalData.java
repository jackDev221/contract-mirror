package org.tron.sunio.contract_mirror.mirror.dao;

import lombok.Data;
import org.springframework.stereotype.Component;
import org.tron.sunio.contract_mirror.mirror.tools.DeepCopyUtils;

import java.math.BigInteger;


@Data
@Component
public class PSMTotalData {
    // PSM 合约全加载完后会统一更新该数据，之后事件时单线程处理 不涉及多线程。
    private boolean finishInit = false;
    private BigInteger totalMaxSwapUSDD = BigInteger.valueOf(0);
    private BigInteger totalSwappedUSDD = BigInteger.valueOf(0);

    public PSMTotalData copySelf() {
        return DeepCopyUtils.deepCopy(this, PSMTotalData.class);
    }
}
