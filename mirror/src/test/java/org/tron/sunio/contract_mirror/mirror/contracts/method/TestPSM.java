package org.tron.sunio.contract_mirror.mirror.contracts.method;

import org.junit.jupiter.api.Test;
import org.springframework.util.Assert;
import org.tron.sunio.contract_mirror.mirror.contracts.impl.PSM;
import org.tron.sunio.contract_mirror.mirror.dao.PSMData;
import org.tron.sunio.contract_mirror.mirror.enums.ContractType;

import java.math.BigInteger;

public class TestPSM {
    @Test
    public void testCalcUSDDToUSDX() {
        PSM psm = new PSM(ContractType.CONTRACT_PSM_USDC, "", "", null,
                null, null, null);
        BigInteger[] res = psm.calcUSDDToUSD(new BigInteger("100000000000000000000"), ContractType.CONTRACT_PSM_USDC, new BigInteger("11234567890123456"));
        Assert.isTrue(res[0].compareTo(new BigInteger("98889024")) == 0, "Value not equal");
        Assert.isTrue(res[1].compareTo(new BigInteger("1110975453716047803")) == 0, "Fee not eqaul");
    }

    @Test
    public void testCalcUSDXToUSDD() {
        PSM psm = new PSM(ContractType.CONTRACT_PSM_USDC, "", "", null,
                null, null, null);
        BigInteger[] res = psm.calcUSDXToUSDD(new BigInteger("1101359"), ContractType.CONTRACT_PSM_USDC, new BigInteger("1234567890123456"));
        Assert.isTrue(res[0].compareTo(new BigInteger("1099999")) == 0, "Value not equal");
        Assert.isTrue(res[1].compareTo(new BigInteger("1358023444567911")) == 0, "Fee not eqaul");
    }

    @Test
    public void tstGetConvertibleAmountUSDX() {
        PSMData psmData = new PSMData();
        psmData.setTout(new BigInteger("11234567890123456"));
        psmData.setReverseLimitEnable(true);
        psmData.setMaxReversSwap(new BigInteger("115792089237316195423570985008687907853269984665640564039457584007913129639935"));
        psmData.setUsdBalance(new BigInteger("100000000"));
        BigInteger res = PSM.getConvertibleAmountUSDX(psmData);
        Assert.isTrue(res.compareTo(new BigInteger("98889024")) == 0, "Value not equal");
    }

    @Test
    public void tstGetConvertibleAmountUSDD() {
        PSMData psmData = new PSMData();
        psmData.setTin(new BigInteger("1234567890123456"));
        psmData.setReverseLimitEnable(true);
        psmData.setMaxSwapUSDD(new BigInteger("115792089237316195423570985008687907853269984665640"));
        psmData.setSwappedUSDD(new BigInteger("12088967448107000000000000"));
        psmData.setTotalSwappedUSDD(new BigInteger("170000123456789012345678"));
        psmData.setTotalMaxSwapUSDD(new BigInteger("21898195115648945516161881"));
        psmData.setUsddBalance(new BigInteger("1101359000000000000"));
        BigInteger res = PSM.getConvertibleAmountUSDD(psmData);
        Assert.isTrue(res.compareTo(new BigInteger("1099999297543101520")) == 0, "Value not equal");
    }

}
