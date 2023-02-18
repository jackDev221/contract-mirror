package org.tron.sunio.contract_mirror.mirror.contracts.event;


import org.junit.jupiter.api.Test;
import org.springframework.util.Assert;
import org.tron.sunio.contract_mirror.event_decode.events.PSMEvent;
import org.tron.sunio.contract_mirror.mirror.contracts.events.IContractEventWrap;
import org.tron.sunio.contract_mirror.mirror.contracts.impl.PSM;
import org.tron.sunio.contract_mirror.mirror.dao.PSMData;
import org.tron.sunio.contract_mirror.mirror.dao.PSMTotalData;
import org.tron.sunio.contract_mirror.mirror.enums.ContractType;
import org.tron.sunio.contract_mirror.mirror.utils.EventLogUtils;

import java.math.BigInteger;

public class TestPSM {

    @Test
    public void testBuyGem() {
        PSM psm = new PSM(ContractType.CONTRACT_PSM_USDC, "TFDUZ6FAtXptkY3dKHk2oRarCckyDYRkEL",
                "TEdsCt5o6vtShpaJ5mxXx7f7pqgNqvie3r", null, null, new PSMTotalData(), PSMEvent.getSigMap());
        PSMData psmData = psm.getPsmData();
        psmData.setReady(true);
        psmData.setAddress("TFDUZ6FAtXptkY3dKHk2oRarCckyDYRkEL");
        psmData.setPolyAddress("TEdsCt5o6vtShpaJ5mxXx7f7pqgNqvie3r");
        psmData.setMaxReversSwap(new BigInteger("115792089237316195423570985008687907853269984665640564039457584007913129639935"));
        psmData.setSwappedUSDD(new BigInteger("12078089655408000000000000"));
        psmData.setTotalMaxSwapUSDD(new BigInteger("170000123456789012345678"));
        psmData.setMaxSwapUSDD(new BigInteger("115792089237316195423570985008687907853269984665640"));
        psmData.setUsdBalance(new BigInteger("22076607301101"));
        psmData.setUsddBalance(new BigInteger("10329186154813826197067392"));
        psmData.setTotalSwappedUSDD(new BigInteger("21886317322949945516161881"));
        psmData.setEnable(true);
        psmData.setReverseLimitEnable(true);
        psmData.setTin(new BigInteger("1234567890123456"));
        psmData.setTout(new BigInteger("11234567890123456"));
        System.out.println(psm.getPsmData());
        psm.setReady(true);
        IContractEventWrap log0 = EventLogUtils.generateContractEvent(
                "tx0",
                new String[]{
                        "085d06ecf4c34b237767a31c0888e121d89546a77f186f1987c6b8715e1a8caa",
                        "0000000000000000000000003802585d6bf577e72284ec4c5e1dc3f0043de734"
                },
                "0000000000000000000000000000000000000000000000000000000005e4ed400000000000000000000000000000000000000000000000000f6afa49f8ec67bb"
        );
        psm.handleEvent(log0);
        System.out.println(psm.getPsmData());
        BigInteger maxReversSwap = new BigInteger("115792089237316195423570985008687907853269984665640564039457584007913129639935");
        BigInteger swappedUSDD = new BigInteger("12077990766384000000000000");
        BigInteger totalMaxSwapUSDD = new BigInteger("170000123456789012345678");
        BigInteger maxSwapUSDD = new BigInteger("115792089237316195423570985008687907853269984665640");
        BigInteger usdBalance = new BigInteger("22076507301102");
        BigInteger usddBalance = new BigInteger("10329286154813279913115195");
        BigInteger totalSwappedUSDD = new BigInteger("21886218433925945516161881");
        Assert.isTrue(maxReversSwap.compareTo(psmData.getMaxReversSwap()) == 0, "maxReversSwap not equal");
        Assert.isTrue(swappedUSDD.compareTo(psmData.getSwappedUSDD()) == 0, "swappedUSDD not equal");
        Assert.isTrue(totalMaxSwapUSDD.compareTo(psmData.getTotalMaxSwapUSDD()) == 0, "totalMaxSwapUSDD not equal");
        Assert.isTrue(maxSwapUSDD.compareTo(psmData.getMaxSwapUSDD()) == 0, "maxSwapUSDD not equal");
        Assert.isTrue(usdBalance.compareTo(psmData.getUsdBalance()) == 0, "usdBalance not equal");
        Assert.isTrue(usddBalance.compareTo(psmData.getUsddBalance()) == 0, "usddBalance not equal");
        Assert.isTrue(totalSwappedUSDD.compareTo(psmData.getTotalSwappedUSDD()) == 0, "totalSwappedUSDD not equal");
    }
}
