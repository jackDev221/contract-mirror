package org.tron.sunio.contract_mirror.mirror.contracts;

import org.junit.jupiter.api.Test;
import org.springframework.util.Assert;
import org.tron.sunio.contract_mirror.event_decode.events.Curve3PoolEvent;
import org.tron.sunio.contract_mirror.mirror.contracts.events.IContractEventWrap;
import org.tron.sunio.contract_mirror.mirror.contracts.impl.Curve3Pool;
import org.tron.sunio.contract_mirror.mirror.dao.CurveBasePoolData;
import org.tron.sunio.contract_mirror.mirror.utils.EventLogUtils;

import java.math.BigInteger;

public class TestCurve3 {
    @Test
    public void testAddLiquidity() {
        Curve3Pool curve3Pool = new Curve3Pool(
                "TAd2UK2c5J4VfMQYxuKL7qqwJsZBocyCfz",
                null,
                3,
                2,
                Curve3PoolEvent.getSigMap()
        );

        CurveBasePoolData data = new CurveBasePoolData();
        data.setReady(true);
        data.setAdminFee(BigInteger.valueOf(5000000000L));
        data.setFee(BigInteger.valueOf(4000000));
        data.setBalance(new BigInteger[3]);
        data.updateBalances(0, new BigInteger("237527249623469474950"));
        data.updateBalances(1, new BigInteger("171670818514848090683"));
        data.updateBalances(2, new BigInteger("5912730454"));
        data.setTotalSupply(new BigInteger("5804442149820345213230"));
        curve3Pool.setCurveBasePoolData(data);
        curve3Pool.setReady(true);

        IContractEventWrap iContractEventWrap1 = EventLogUtils.generateContractEvent("txId1", new String[]{
                        "423f6495a08fc652425cf4ed0d1f9e37e571d9b9529b1c1c23cce780b2e7df0d",
                        "0000000000000000000000003802585d6bf577e72284ec4c5e1dc3f0043de734"
                },
                "000000000000000000000000000000000000000000000004fcc1a89027f000000000000000000000000000000000000000" +
                        "000000000000067374ed82cf7c00000000000000000000000000000000000000000000000000000000000005f5e1000" +
                        "000000000000000000000000000000000000000000000000028dff2d723991100000000000000000000000000000000000000" +
                        "00000000000039860feace771d000000000000000000000000000000000000000000000000000000000000a48b000000000000" +
                        "0000000000000000000000000000000001628d2c5ce8435d707000000000000000000000000000000000000000000000014ee" +
                        "c02323944b3a38e");
        curve3Pool.handleEvent(iContractEventWrap1);
        BigInteger balance0 = new BigInteger("329521497006893004798");
        BigInteger balance1 = new BigInteger("290662722776550821549");
        BigInteger balance2 = new BigInteger("6012709393");
        BigInteger totalSupply = new BigInteger("6178218730983441802126");
        Assert.isTrue(curve3Pool.getCurveBasePoolData().getBalance()[0].compareTo(balance0) == 0, "balance0 not equal");
        Assert.isTrue(curve3Pool.getCurveBasePoolData().getBalance()[1].compareTo(balance1) == 0, "balance1 not equal");
        Assert.isTrue(curve3Pool.getCurveBasePoolData().getBalance()[2].compareTo(balance2) == 0, "balance2 not equal");
        Assert.isTrue(curve3Pool.getCurveBasePoolData().getTotalSupply().compareTo(totalSupply) == 0, "totalSupply not equal");
    }

    @Test
    public void testTokenExchange() {
        Curve3Pool curve3Pool = new Curve3Pool(
                "TAd2UK2c5J4VfMQYxuKL7qqwJsZBocyCfz",
                null,
                3,
                2,
                Curve3PoolEvent.getSigMap()
        );

        CurveBasePoolData data = new CurveBasePoolData();
        data.setReady(true);
        data.setAdminFee(BigInteger.valueOf(5000000000L));
        data.setFee(BigInteger.valueOf(4000000));
        data.setBalance(new BigInteger[3]);
        data.updateBalances(0, new BigInteger("329521497006893004798"));
        data.updateBalances(1, new BigInteger("290662722776550821549"));
        data.updateBalances(2, new BigInteger("6012709393"));
        data.setTotalSupply(new BigInteger("6178218730983441802126"));
        curve3Pool.setCurveBasePoolData(data);
        curve3Pool.setReady(true);

        IContractEventWrap iContractEventWrap1 = EventLogUtils.generateContractEvent("txId1", new String[]{
                "8b3e96f2b889fa771c53c981b40daf005f63f637f1869f707052d15a3dd97140",
                "0000000000000000000000003802585d6bf577e72284ec4c5e1dc3f0043de734"
                },
                "0000000000000000000000000000000000000000000000000000000000000002000000000000000000000000000000000" +
                        "0000000000000000000000000b71b000000000000000000000000000000000000000000000000000000000000000000" +
                        "00000000000000000000000000000000000000000000000081925a2f5af8d202");
        curve3Pool.handleEvent(iContractEventWrap1);
        BigInteger balance0 = new BigInteger("320183004797908118178");
        BigInteger balance1 = new BigInteger("290662722776550821549");
        BigInteger balance2 = new BigInteger("6024709393");
        System.out.println(curve3Pool.getCurveBasePoolData());
        BigInteger totalSupply = new BigInteger("6178218730983441802126");
        Assert.isTrue(curve3Pool.getCurveBasePoolData().getBalance()[0].compareTo(balance0) == 0, "balance0 not equal");
        Assert.isTrue(curve3Pool.getCurveBasePoolData().getBalance()[1].compareTo(balance1) == 0, "balance1 not equal");
        Assert.isTrue(curve3Pool.getCurveBasePoolData().getBalance()[2].compareTo(balance2) == 0, "balance2 not equal");
        Assert.isTrue(curve3Pool.getCurveBasePoolData().getTotalSupply().compareTo(totalSupply) == 0, "totalSupply not equal");
    }
    @Test
    public void testRemoveLiquidity() {
        Curve3Pool curve3Pool = new Curve3Pool(
                "TAd2UK2c5J4VfMQYxuKL7qqwJsZBocyCfz",
                null,
                3,
                2,
                Curve3PoolEvent.getSigMap()
        );

        CurveBasePoolData data = new CurveBasePoolData();
        data.setReady(true);
        data.setAdminFee(BigInteger.valueOf(5000000000L));
        data.setFee(BigInteger.valueOf(4000000));
        data.setBalance(new BigInteger[3]);
        data.updateBalances(0, new BigInteger("320183004797908118178"));
        data.updateBalances(1, new BigInteger("290662722776550821549"));
        data.updateBalances(2, new BigInteger("6024709393"));
        data.setTotalSupply(new BigInteger("6178218730983441802126"));
        curve3Pool.setCurveBasePoolData(data);
        curve3Pool.setReady(true);

        IContractEventWrap iContractEventWrap1 = EventLogUtils.generateContractEvent("txId1", new String[]{
                "a49d4cf02656aebf8c771f5a8585638a2a15ee6c97cf7205d4208ed7c1df252d",
                "0000000000000000000000003802585d6bf577e72284ec4c5e1dc3f0043de734"
                },
                "000000000000000000000000000000000000000000000002951e901ac5abf8c6000000000000000000000000000000" +
                        "000000000000000002582a579b99f96a18000000000000000000000000000000000000000000000000000000003" +
                        "56dd741000000000000000000000000000000000000000000000000000000000000000000000000000000000000" +
                        "000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000" +
                        "00000000000000000000000000000000000000000000000000000000000011d171db6961a382657");
        curve3Pool.handleEvent(iContractEventWrap1);
        BigInteger balance0 = new BigInteger("272544332444876501468");
        BigInteger balance1 = new BigInteger("247416248078957405333");
        BigInteger balance2 = new BigInteger("5128318416");
        BigInteger totalSupply = new BigInteger("5258987749200220399191");
        Assert.isTrue(curve3Pool.getCurveBasePoolData().getBalance()[0].compareTo(balance0) == 0, "balance0 not equal");
        Assert.isTrue(curve3Pool.getCurveBasePoolData().getBalance()[1].compareTo(balance1) == 0, "balance1 not equal");
        Assert.isTrue(curve3Pool.getCurveBasePoolData().getBalance()[2].compareTo(balance2) == 0, "balance2 not equal");
        Assert.isTrue(curve3Pool.getCurveBasePoolData().getTotalSupply().compareTo(totalSupply) == 0, "totalSupply not equal");
    }

    @Test
    public void TestRemoveLiquidityInBalance() {
        Curve3Pool curve3Pool = new Curve3Pool(
                "TAd2UK2c5J4VfMQYxuKL7qqwJsZBocyCfz",
                null,
                3,
                2,
                Curve3PoolEvent.getSigMap()
        );

        CurveBasePoolData data = new CurveBasePoolData();
        data.setReady(true);
        data.setAdminFee(BigInteger.valueOf(5000000000L));
        data.setFee(BigInteger.valueOf(4000000));
        data.setBalance(new BigInteger[3]);
        data.updateBalances(0, new BigInteger("272544332444876501468"));
        data.updateBalances(1, new BigInteger("247416248078957405333"));
        data.updateBalances(2, new BigInteger("5128318416"));
        data.setTotalSupply(new BigInteger("5258987749200220399191"));
        curve3Pool.setCurveBasePoolData(data);
        curve3Pool.setReady(true);

        IContractEventWrap iContractEventWrap1 = EventLogUtils.generateContractEvent("txId1", new String[]{
                "173599dbf9c6ca6f7c3b590df07ae98a45d74ff54065505141e7de6c46a624c2",
                "0000000000000000000000003802585d6bf577e72284ec4c5e1dc3f0043de734"
                },
                "00000000000000000000000000000000000000000000000001312c9990fec5b90000000000000000000000000000000000" +
                        "000000000000000121b4ee4030fc7400000000000000000000000000000000000000000000000000000000000711360" +
                        "0000000000000000000000000000000000000000000000000000759fb2b1fc8000000000000000000000000000000000" +
                        "0000000000000000000072901f7eaf300000000000000000000000000000000000000000000000000000000000000140" +
                        "0000000000000000000000000000000000000000000012dc31e2698c936618900000000000000000000000000000000000" +
                        "000000000011d0e8b131e445fdc6a");
        curve3Pool.handleEvent(iContractEventWrap1);
        BigInteger balance0 = new BigInteger("272458429497381821503");
        BigInteger balance1 = new BigInteger("247334698938961818280");
        BigInteger balance2 = new BigInteger("5127855248");
        BigInteger totalSupply = new BigInteger("5258370013366233062506");
        Assert.isTrue(curve3Pool.getCurveBasePoolData().getBalance()[0].compareTo(balance0) == 0, "balance0 not equal");
        Assert.isTrue(curve3Pool.getCurveBasePoolData().getBalance()[1].compareTo(balance1) == 0, "balance1 not equal");
        Assert.isTrue(curve3Pool.getCurveBasePoolData().getBalance()[2].compareTo(balance2) == 0, "balance2 not equal");
        Assert.isTrue(curve3Pool.getCurveBasePoolData().getTotalSupply().compareTo(totalSupply) == 0, "totalSupply not equal");
    }
}
