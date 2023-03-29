package org.tron.sunio.contract_mirror.mirror.contracts.event;

import org.junit.jupiter.api.Test;
import org.springframework.util.Assert;
import org.tron.sunio.contract_mirror.event_decode.events.Curve2PoolEvent;
import org.tron.sunio.contract_mirror.mirror.contracts.events.IContractEventWrap;
import org.tron.sunio.contract_mirror.mirror.contracts.impl.OldCurvePool;
import org.tron.sunio.contract_mirror.mirror.dao.OldCurvePoolData;
import org.tron.sunio.contract_mirror.mirror.enums.ContractType;
import org.tron.sunio.contract_mirror.mirror.utils.EventLogUtils;

import java.math.BigInteger;

public class TestCurve2 {
    @Test
    public void testTokenExchange() {
        OldCurvePool curve2Pool = new OldCurvePool(
                "TNTfaTpkdd4AQDeqr8SGG7tgdkdjdhbP5c",
                ContractType.CONTRACT_CURVE_2POOL,
                "",
                null,
                null,
                2,
                1,
                Curve2PoolEvent.getSigMap()
        );

        OldCurvePoolData data = new OldCurvePoolData();
        data.setReady(true);
        data.setAdminFee(BigInteger.valueOf(5000000000L));
        data.setFee(BigInteger.valueOf(4000000));
        data.setBalances(new BigInteger[2]);
        data.updateBalances(0, new BigInteger("19857992243706"));
        data.updateBalances(1, new BigInteger("8632221947644"));
        curve2Pool.setOldCurvePoolData(data);
        curve2Pool.setReady(true);

        IContractEventWrap iContractEventWrap1 = EventLogUtils.generateContractEvent("txId1", new String[]{
                        "8b3e96f2b889fa771c53c981b40daf005f63f637f1869f707052d15a3dd97140",
                        "0000000000000000000000003802585d6bf577e72284ec4c5e1dc3f0043de734"
                },
                "0x000000000000000000000000000000000000000000000000000000000000000100000000000000000000000000000000" +
                        "000000000000000000000000000f42400000000000000000000000000000000000000000000000000000000000000000" +
                        "00000000000000000000000000000000000000000000000000000000000f44fe");

        IContractEventWrap iContractEventWrap2 = EventLogUtils.generateContractEvent("txId2", new String[]{
                        "8b3e96f2b889fa771c53c981b40daf005f63f637f1869f707052d15a3dd97140",
                        "0000000000000000000000003802585d6bf577e72284ec4c5e1dc3f0043de734"
                },
                "0x00000000000000000000000000000000000000000000000000000000000000010000000000000000000000000000000000" +
                        "0000000000000000000000497e1640000000000000000000000000000000000000000000000000000000000000000000" +
                        "000000000000000000000000000000000000000000000000000000498b4d75");


        IContractEventWrap iContractEventWrap3 = EventLogUtils.generateContractEvent("txId2", new String[]{
                        "8b3e96f2b889fa771c53c981b40daf005f63f637f1869f707052d15a3dd97140",
                        "0000000000000000000000003802585d6bf577e72284ec4c5e1dc3f0043de734"
                },
                "0x000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000" +
                        "00000000000000000000000007562622000000000000000000000000000000000000000000000000000000000000000" +
                        "1000000000000000000000000000000000000000000000000000000000753546d");

        curve2Pool.handleEvent(iContractEventWrap1);
        curve2Pool.handleEvent(iContractEventWrap2);
        curve2Pool.handleEvent(iContractEventWrap3);
        BigInteger balance0 = new BigInteger("19856880216202");
        BigInteger balance1 = new BigInteger("8633333021441");
        OldCurvePoolData poolData = curve2Pool.getOldCurvePoolData();
        Assert.isTrue(poolData.getBalances()[0].compareTo(balance0) == 0, "balance0 not equal");
        Assert.isTrue(poolData.getBalances()[1].compareTo(balance1) == 0, "balance1 not equal");
    }

    @Test
    public void testAddLiquidity() {
        OldCurvePool curve2Pool = new OldCurvePool(
                "TNTfaTpkdd4AQDeqr8SGG7tgdkdjdhbP5c",
                ContractType.CONTRACT_CURVE_2POOL,
                "",
                null,
                null,
                2,
                1,
                Curve2PoolEvent.getSigMap()
        );


        OldCurvePoolData data = new OldCurvePoolData();
        data.setReady(true);
        data.setAdminFee(BigInteger.valueOf(5000000000L));
        data.setFee(BigInteger.valueOf(4000000));
        data.setBalances(new BigInteger[2]);
        data.updateBalances(0, new BigInteger("19856880216202"));
        data.updateBalances(1, new BigInteger("8633333021441"));
        data.setTotalSupply(new BigInteger("90691368356438248068362"));
        curve2Pool.setOldCurvePoolData(data);
        curve2Pool.setReady(true);

        IContractEventWrap iContractEventWrap1 = EventLogUtils.generateContractEvent("txId1", new String[]{
                        "26f55a85081d24974e85c6c00045d0f0453991e95873f52bff0d21af4079a768",
                        "0000000000000000000000003802585d6bf577e72284ec4c5e1dc3f0043de734"
                },
                "0000000000000000000000000000000000000000000000000000000001f78a400" +
                        "000000000000000000000000000000000000000000000000000000003473bc00000000000000000000" +
                        "0000000000000000000000000000000000000000016270000000000000000000000000000000000000000000000" +
                        "000000000000001620000000000000000000000000000000000000000000179080a5d7f6938379e0d50000000000000000" +
                        "00000000000000000000000000001334677457244cb0ec5f");
        curve2Pool.handleEvent(iContractEventWrap1);
        BigInteger balance0 = new BigInteger("19856913213367");
        BigInteger balance1 = new BigInteger("8633388018609");
        BigInteger totalSupply = new BigInteger("90691648545452777860191");
        OldCurvePoolData poolData = curve2Pool.getOldCurvePoolData();
        Assert.isTrue(poolData.getBalances()[0].compareTo(balance0) == 0, "balance0 not equal");
        Assert.isTrue(poolData.getBalances()[1].compareTo(balance1) == 0, "balance1 not equal");
        Assert.isTrue(poolData.getTotalSupply().compareTo(totalSupply) == 0, "totalSupply not equal");
    }

    @Test
    public void TestRemoveLiquidity(){
        OldCurvePool curve2Pool = new OldCurvePool(
                "TNTfaTpkdd4AQDeqr8SGG7tgdkdjdhbP5c",
                ContractType.CONTRACT_CURVE_2POOL,
                "",
                null,
                null,
                2,
                1,
                Curve2PoolEvent.getSigMap()
        );

        OldCurvePoolData data = new OldCurvePoolData();
        data.setReady(true);
        data.setAdminFee(BigInteger.valueOf(5000000000L));
        data.setFee(BigInteger.valueOf(4000000));
        data.setBalances(new BigInteger[2]);
        data.updateBalances(0, new BigInteger("19728237545909"));
        data.updateBalances(1, new BigInteger("8577445960799"));
        data.setTotalSupply(new BigInteger("90103964845060825324293"));
        curve2Pool.setOldCurvePoolData(data);
        curve2Pool.setReady(true);

        IContractEventWrap iContractEventWrap1 = EventLogUtils.generateContractEvent("txId1", new String[]{
                        "7c363854ccf79623411f8995b362bce5eddff18c927edc6f5dbbb5e05819a82c",
                        "0000000000000000000000003802585d6bf577e72284ec4c5e1dc3f0043de734"
                },
                "000000000000000000000000000000000000000000000000000000063c09001d00000000000000000000000000000000000000000000000000000002b5eca5330000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000130dea7c08c712ea5ace");
        curve2Pool.handleEvent(iContractEventWrap1);
        BigInteger balance0 = new BigInteger("19701460519320");
        BigInteger balance1 = new BigInteger("8565803841324");//8565803841324
        BigInteger totalSupply = new BigInteger("89981667237034579352270");
        OldCurvePoolData poolData = curve2Pool.getOldCurvePoolData();
        Assert.isTrue(poolData.getBalances()[0].compareTo(balance0) == 0, "balance0 not equal");
        Assert.isTrue(poolData.getBalances()[1].compareTo(balance1) == 0, "balance1 not equal");
        Assert.isTrue(poolData.getTotalSupply().compareTo(totalSupply) == 0, "totalSupply not equal");
    }

    @Test
    public void TestRemoveLiquidityInBalance(){
        OldCurvePool curve2Pool = new OldCurvePool(
                "TNTfaTpkdd4AQDeqr8SGG7tgdkdjdhbP5c",
                ContractType.CONTRACT_CURVE_2POOL,
                "",
                null,
                null,
                2,
                1,
                Curve2PoolEvent.getSigMap()
        );


        OldCurvePoolData data = new OldCurvePoolData();
        data.setReady(true);
        data.setAdminFee(BigInteger.valueOf(5000000000L));
        data.setFee(BigInteger.valueOf(4000000));
        data.setBalances(new BigInteger[2]);
        data.updateBalances(0, new BigInteger("19664183778008"));
        data.updateBalances(1, new BigInteger("8565803841324"));
        data.setTotalSupply(new BigInteger("89863038557249120759408"));
        curve2Pool.setOldCurvePoolData(data);
        curve2Pool.setReady(true);

        IContractEventWrap iContractEventWrap1 = EventLogUtils.generateContractEvent("txId1", new String[]{
                        "2b5508378d7e19e0d5fa338419034731416c4f5b219a10379956f764317fd47e",
                        "0000000000000000000000003802585d6bf577e72284ec4c5e1dc3f0043de734"
                },
                "0000000000000000000000000000000000000000000000000000000000004192000000000000000000000000000000000000000000000000000000000000302d00000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000175962fc3155dd68bd39750000000000000000000000000000000000000000000013077c2df8b833b91a34");
        curve2Pool.handleEvent(iContractEventWrap1);
        BigInteger balance0 = new BigInteger("19664183761222");
        BigInteger balance1 = new BigInteger("8565803828991");//8565803841324
        BigInteger totalSupply = new BigInteger("89863038464543906404916");
        OldCurvePoolData poolData = curve2Pool.getOldCurvePoolData();
        Assert.isTrue(poolData.getBalances()[0].compareTo(balance0) == 0, "balance0 not equal");
        Assert.isTrue(poolData.getBalances()[1].compareTo(balance1) == 0, "balance1 not equal");
        Assert.isTrue(poolData.getTotalSupply().compareTo(totalSupply) == 0, "totalSupply not equal");
    }
}
