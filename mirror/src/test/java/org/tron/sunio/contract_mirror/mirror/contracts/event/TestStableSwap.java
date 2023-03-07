package org.tron.sunio.contract_mirror.mirror.contracts.event;

import org.junit.jupiter.api.Test;
import org.springframework.util.Assert;
import org.tron.sunio.contract_mirror.event_decode.events.Curve2PoolEvent;
import org.tron.sunio.contract_mirror.mirror.contracts.events.IContractEventWrap;
import org.tron.sunio.contract_mirror.mirror.contracts.impl.BaseStableSwapPool;
import org.tron.sunio.contract_mirror.mirror.contracts.impl.CurveBasePool;
import org.tron.sunio.contract_mirror.mirror.dao.CurveBasePoolData;
import org.tron.sunio.contract_mirror.mirror.dao.StableSwapPoolData;
import org.tron.sunio.contract_mirror.mirror.enums.ContractType;
import org.tron.sunio.contract_mirror.mirror.utils.ContractsHelper;
import org.tron.sunio.contract_mirror.mirror.utils.EventLogUtils;

import java.math.BigInteger;

public class TestStableSwap {
    @Test
    public void testExchangeUnderlying() {
        // tx: da9214802ef340ef7d8c081588fda8d4a5199c65db0ed12160cc6576833da15e
        long timestamp = 1677048099;
        ContractsHelper contractsHelper = new ContractsHelper();
        CurveBasePool curve3Pool = new CurveBasePool(
                "TW6tDjosBGXpjnRMQnHAhPuqTubGs93B9E",
                ContractType.CONTRACT_CURVE_2POOL,
                null,
                null,
                3,
                2,
                "",
                Curve2PoolEvent.getSigMap()
        );
        CurveBasePoolData data = new CurveBasePoolData();
        data.setReady(true);
        data.setAdminFee(BigInteger.valueOf(5000000000L));
        data.setFee(BigInteger.valueOf(4000000));
        data.setBalances(new BigInteger[3]);
        data.updateBalances(0, new BigInteger("496017383905171617797"));
        data.updateBalances(1, new BigInteger("198780233443220411454527"));
        data.updateBalances(2, new BigInteger("851085251952"));
        data.setInitialA(new BigInteger("1500"));
        data.setInitialATime(new BigInteger("0"));
        data.setFutureA(new BigInteger("1500"));
        data.setFutureATime(new BigInteger("0"));
        data.setTotalSupply(new BigInteger("934127471967327020478908"));

        curve3Pool.setCurveBasePoolData(data);
        curve3Pool.setReady(true);

        contractsHelper.getContractMaps().put("TW6tDjosBGXpjnRMQnHAhPuqTubGs93B9E", curve3Pool);
        contractsHelper.setBlockTime(timestamp);

        StableSwapPoolData stableSwapPoolData = new StableSwapPoolData(2, 3);
        stableSwapPoolData.setReady(true);
        stableSwapPoolData.setAdminFee(BigInteger.valueOf(5000000000L));
        stableSwapPoolData.setFee(BigInteger.valueOf(4000000));
        stableSwapPoolData.updateBalances(0, new BigInteger("1856057139"));
        stableSwapPoolData.updateBalances(1, new BigInteger("214336452273731588955638"));
        stableSwapPoolData.setBasePool("TW6tDjosBGXpjnRMQnHAhPuqTubGs93B9E");
        stableSwapPoolData.setBaseCacheUpdated(new BigInteger("1678175934"));
        stableSwapPoolData.setInitialA(new BigInteger("150000"));
        stableSwapPoolData.setInitialATime(new BigInteger("1627548462"));
        stableSwapPoolData.setFutureA(new BigInteger("15000"));
        stableSwapPoolData.setFutureATime(new BigInteger("1627641643"));
        stableSwapPoolData.setBaseVirtualPrice(new BigInteger("1033463465560439641"));
        stableSwapPoolData.setLpTotalSupply(new BigInteger("89037695813561108584431"));

        BaseStableSwapPool baseStableSwapPool = new BaseStableSwapPool(
                "TJ9d9GXCsF7cmnHDLrtT4qo6kNbVK7Qebr",
                ContractType.STABLE_SWAP_POOL,
                2,
                2,
                new BigInteger[]{new BigInteger("1000000000000000000000000000000"), new BigInteger("1000000000000000000")},
                new BigInteger[]{new BigInteger("1000000000000"), new BigInteger("1")},
                "",
                null,
                contractsHelper,
                Curve2PoolEvent.getSigMap()
        );

        baseStableSwapPool.setStableSwapPoolData(stableSwapPoolData);
        baseStableSwapPool.setReady(true);

        IContractEventWrap ic1 = EventLogUtils.generateContractEvent(
                "txid0",
                new String[]{
                        "d013ca23e77a65003c2c659c5442c00c805371b7fc1ebd4c206c41d1536bd90b",
                        "0000000000000000000000003802585d6bf577e72284ec4c5e1dc3f0043de734"
                },
                "00000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000005f5e100000000000000000000000000000000000000000000000000000000000000000100000000000000000000000000000000000000000000000064b1962b722f3a7f",
                1678177164000L
        );

        baseStableSwapPool.handleEvent(ic1);
        BigInteger balance0 = new BigInteger("1956057139");
        BigInteger balance1 = new BigInteger("213429387371073320501560");
        BigInteger desTimestamp = new BigInteger("1678177164");
//        System.out.println(stableSwapPoolData);
        Assert.isTrue(balance0.compareTo(stableSwapPoolData.getBalances()[0]) == 0, "balance0 not equal");
        Assert.isTrue(balance1.compareTo(stableSwapPoolData.getBalances()[1]) == 0, "balance1 not equal");
        Assert.isTrue(desTimestamp.compareTo(stableSwapPoolData.getBaseCacheUpdated()) == 0, "baseCacheUpdated not equal");
    }

    @Test
    public void testExchangeUnderlying1() {
        // tx: fe70f9ae62a57647f51e201ee20743ee062edcad1bae54b6981fa7a0f7403614
        long timestamp = 1678177164L;
        ContractsHelper contractsHelper = new ContractsHelper();
        CurveBasePool curve3Pool = new CurveBasePool(
                "TW6tDjosBGXpjnRMQnHAhPuqTubGs93B9E",
                ContractType.CONTRACT_CURVE_2POOL,
                null,
                null,
                3,
                2,
                "",
                Curve2PoolEvent.getSigMap()
        );
        CurveBasePoolData data = new CurveBasePoolData();
        data.setReady(true);
        data.setAdminFee(BigInteger.valueOf(5000000000L));
        data.setFee(BigInteger.valueOf(4000000));
        data.setBalances(new BigInteger[3]);
        data.updateBalances(0, new BigInteger("488760627928326498224"));
        data.updateBalances(1, new BigInteger("198780233443220411454527"));
        data.updateBalances(2, new BigInteger("851085251952"));
        data.setInitialA(new BigInteger("1500"));
        data.setInitialATime(new BigInteger("0"));
        data.setFutureA(new BigInteger("1500"));
        data.setFutureATime(new BigInteger("0"));
        data.setTotalSupply(new BigInteger("933220588513939137755666"));

        curve3Pool.setCurveBasePoolData(data);
        curve3Pool.setReady(true);

        contractsHelper.getContractMaps().put("TW6tDjosBGXpjnRMQnHAhPuqTubGs93B9E", curve3Pool);
        contractsHelper.setBlockTime(timestamp);

        StableSwapPoolData stableSwapPoolData = new StableSwapPoolData(2, 3);
        stableSwapPoolData.setReady(true);
        stableSwapPoolData.setAdminFee(BigInteger.valueOf(5000000000L));
        stableSwapPoolData.setFee(BigInteger.valueOf(4000000));
        stableSwapPoolData.updateBalances(0, new BigInteger("1956057139"));
        stableSwapPoolData.updateBalances(1, new BigInteger("213429387371073320501560"));
        stableSwapPoolData.setBasePool("TW6tDjosBGXpjnRMQnHAhPuqTubGs93B9E");
        stableSwapPoolData.setBaseCacheUpdated(new BigInteger("1678177164"));
        stableSwapPoolData.setInitialA(new BigInteger("150000"));
        stableSwapPoolData.setInitialATime(new BigInteger("1627548462"));
        stableSwapPoolData.setFutureA(new BigInteger("15000"));
        stableSwapPoolData.setFutureATime(new BigInteger("1627641643"));
        stableSwapPoolData.setBaseVirtualPrice(new BigInteger("1033463465560439641"));
        stableSwapPoolData.setLpTotalSupply(new BigInteger("89037695813561108584431"));

        BaseStableSwapPool baseStableSwapPool = new BaseStableSwapPool(
                "TJ9d9GXCsF7cmnHDLrtT4qo6kNbVK7Qebr",
                ContractType.STABLE_SWAP_POOL,
                3,
                2,
                new BigInteger[]{new BigInteger("1000000000000000000000000000000"), new BigInteger("1000000000000000000")},
                new BigInteger[]{new BigInteger("1000000000000"), new BigInteger("1")},
                "",
                null,
                contractsHelper,
                Curve2PoolEvent.getSigMap()
        );

        baseStableSwapPool.setStableSwapPoolData(stableSwapPoolData);
        baseStableSwapPool.setReady(true);

        IContractEventWrap ic1 = EventLogUtils.generateContractEvent(
                "txid0",
                new String[]{
                        "d013ca23e77a65003c2c659c5442c00c805371b7fc1ebd4c206c41d1536bd90b",
                        "0000000000000000000000003802585d6bf577e72284ec4c5e1dc3f0043de734"
                },
                "00000000000000000000000000000000000000000000000000000000000000010000000000000000000000000000000000000000000000056bc75e2d63100000000000000000000000000000000000000000000000000000000000000000000200000000000000000000000000000000000000000000024244a71aedce3eb56e",
                1678178694000L
        );

        baseStableSwapPool.handleEvent(ic1);
        BigInteger balance0 = new BigInteger("1956057139");
        BigInteger balance1 = new BigInteger("213429387371073320501560");
        BigInteger desTimestamp = new BigInteger("1678178694");
        System.out.println(stableSwapPoolData);
        Assert.isTrue(balance0.compareTo(stableSwapPoolData.getBalances()[0]) == 0, "balance0 not equal");
        Assert.isTrue(balance1.compareTo(stableSwapPoolData.getBalances()[1]) == 0, "balance1 not equal");
        Assert.isTrue(desTimestamp.compareTo(stableSwapPoolData.getBaseCacheUpdated()) == 0, "baseCacheUpdated not equal");
      }

    @Test
    public void testAddLiquidity() {
        // tx: 5d21ea255a74205bc714a825a10a9f234670201c9a1b82099c391f8042fd4310
        long timestamp = 1678178694L;
        ContractsHelper contractsHelper = new ContractsHelper();
        CurveBasePool curve3Pool = new CurveBasePool(
                "TW6tDjosBGXpjnRMQnHAhPuqTubGs93B9E",
                ContractType.CONTRACT_CURVE_2POOL,
                null,
                null,
                3,
                2,
                "",
                Curve2PoolEvent.getSigMap()
        );
        CurveBasePoolData data = new CurveBasePoolData();
        data.setReady(true);
        data.setAdminFee(BigInteger.valueOf(5000000000L));
        data.setFee(BigInteger.valueOf(4000000));
        data.setBalances(new BigInteger[3]);
        data.updateBalances(0, new BigInteger("588760627928326498224"));
        data.updateBalances(1, new BigInteger("188110934129571857328207"));
        data.updateBalances(2, new BigInteger("851085251952"));
        data.setInitialA(new BigInteger("1500"));
        data.setInitialATime(new BigInteger("0"));
        data.setFutureA(new BigInteger("1500"));
        data.setFutureATime(new BigInteger("0"));
        data.setTotalSupply(new BigInteger("933220588513939137755666"));

        curve3Pool.setCurveBasePoolData(data);
        curve3Pool.setReady(true);

        contractsHelper.getContractMaps().put("TW6tDjosBGXpjnRMQnHAhPuqTubGs93B9E", curve3Pool);
        contractsHelper.setBlockTime(timestamp);

        StableSwapPoolData stableSwapPoolData = new StableSwapPoolData(2, 3);
        stableSwapPoolData.setReady(true);
        stableSwapPoolData.setAdminFee(BigInteger.valueOf(5000000000L));
        stableSwapPoolData.setFee(BigInteger.valueOf(4000000));
        stableSwapPoolData.updateBalances(0, new BigInteger("2033050167"));
        stableSwapPoolData.updateBalances(1, new BigInteger("213619328521827776258634"));
        stableSwapPoolData.setBasePool("TW6tDjosBGXpjnRMQnHAhPuqTubGs93B9E");
        stableSwapPoolData.setBaseCacheUpdated(new BigInteger("1678180476"));
        stableSwapPoolData.setInitialA(new BigInteger("150000"));
        stableSwapPoolData.setInitialATime(new BigInteger("1627548462"));
        stableSwapPoolData.setFutureA(new BigInteger("15000"));
        stableSwapPoolData.setFutureATime(new BigInteger("1627641643"));
        stableSwapPoolData.setBaseVirtualPrice(new BigInteger("1033466048108359893"));
        stableSwapPoolData.setLpTotalSupply(new BigInteger("89362732367941936322532"));

        BaseStableSwapPool baseStableSwapPool = new BaseStableSwapPool(
                "TJ9d9GXCsF7cmnHDLrtT4qo6kNbVK7Qebr",
                ContractType.STABLE_SWAP_POOL,
                3,
                2,
                new BigInteger[]{new BigInteger("1000000000000000000000000000000"), new BigInteger("1000000000000000000")},
                new BigInteger[]{new BigInteger("1000000000000"), new BigInteger("1")},
                "",
                null,
                contractsHelper,
                Curve2PoolEvent.getSigMap()
        );

        baseStableSwapPool.setStableSwapPoolData(stableSwapPoolData);
        baseStableSwapPool.setReady(true);

        IContractEventWrap ic1 = EventLogUtils.generateContractEvent(
                "txid0",
                new String[]{
                        "26f55a85081d24974e85c6c00045d0f0453991e95873f52bff0d21af4079a768",
                        "0000000000000000000000003802585d6bf577e72284ec4c5e1dc3f0043de734"
                },
                "000000000000000000000000000000000000000000000000000000000112a880000000000000000000000000000000000000000000000002d1a51c7e005000000000000000000000000000000000000000000000000000000000000000000cb1000000000000000000000000000000000000000000000000005dfe3c893197a5000000000000000000000000000000000000000000002be03156ab73bce47cec0000000000000000000000000000000000000000000012f08a9a5b1a991828b8",
                1678178694000L
        );

        baseStableSwapPool.handleEvent(ic1);
        BigInteger balance0 = new BigInteger("2051048543");
        BigInteger balance1 = new BigInteger("213671315293473382602360");
        BigInteger totalSupply = new BigInteger("89439802664637349505208");
        System.out.println(stableSwapPoolData);
        Assert.isTrue(balance0.compareTo(stableSwapPoolData.getBalances()[0]) == 0, "balance0 not equal");
        Assert.isTrue(balance1.compareTo(stableSwapPoolData.getBalances()[1]) == 0, "balance1 not equal");
        Assert.isTrue(totalSupply.compareTo(stableSwapPoolData.getLpTotalSupply()) == 0, "totalSupply not equal");
    }


    @Test
    public void testRemoveLiquidityImbalance() {
        // tx:18c0e2ecc50d259f23556d5b82b160b02dd93c77ceb36c0772ca5d3a7765f9eb
        long timestamp = 1678178694L;
        ContractsHelper contractsHelper = new ContractsHelper();
        CurveBasePool curve3Pool = new CurveBasePool(
                "TW6tDjosBGXpjnRMQnHAhPuqTubGs93B9E",
                ContractType.CONTRACT_CURVE_2POOL,
                null,
                null,
                3,
                2,
                "",
                Curve2PoolEvent.getSigMap()
        );
        CurveBasePoolData data = new CurveBasePoolData();
        data.setReady(true);
        data.setAdminFee(BigInteger.valueOf(5000000000L));
        data.setFee(BigInteger.valueOf(4000000));
        data.setBalances(new BigInteger[3]);
        data.updateBalances(0, new BigInteger("588760627928326498224"));
        data.updateBalances(1, new BigInteger("188110934129571857328207"));
        data.updateBalances(2, new BigInteger("851085251952"));
        data.setInitialA(new BigInteger("1500"));
        data.setInitialATime(new BigInteger("0"));
        data.setFutureA(new BigInteger("1500"));
        data.setFutureATime(new BigInteger("0"));
        data.setTotalSupply(new BigInteger("933220588513939137755666"));

        curve3Pool.setCurveBasePoolData(data);
        curve3Pool.setReady(true);

        contractsHelper.getContractMaps().put("TW6tDjosBGXpjnRMQnHAhPuqTubGs93B9E", curve3Pool);
        contractsHelper.setBlockTime(timestamp);

        StableSwapPoolData stableSwapPoolData = new StableSwapPoolData(2, 3);
        stableSwapPoolData.setReady(true);
        stableSwapPoolData.setAdminFee(BigInteger.valueOf(5000000000L));
        stableSwapPoolData.setFee(BigInteger.valueOf(4000000));
        stableSwapPoolData.updateBalances(0, new BigInteger("2051048543"));
        stableSwapPoolData.updateBalances(1, new BigInteger("213671315293473382602360"));
        stableSwapPoolData.setBasePool("TW6tDjosBGXpjnRMQnHAhPuqTubGs93B9E");
        stableSwapPoolData.setBaseCacheUpdated(new BigInteger("1678180476"));
        stableSwapPoolData.setInitialA(new BigInteger("150000"));
        stableSwapPoolData.setInitialATime(new BigInteger("1627548462"));
        stableSwapPoolData.setFutureA(new BigInteger("15000"));
        stableSwapPoolData.setFutureATime(new BigInteger("1627641643"));
        stableSwapPoolData.setBaseVirtualPrice(new BigInteger("1033466048108359893"));
        stableSwapPoolData.setLpTotalSupply(new BigInteger("89439802664637349505208"));

        BaseStableSwapPool baseStableSwapPool = new BaseStableSwapPool(
                "TJ9d9GXCsF7cmnHDLrtT4qo6kNbVK7Qebr",
                ContractType.STABLE_SWAP_POOL,
                3,
                2,
                new BigInteger[]{new BigInteger("1000000000000000000000000000000"), new BigInteger("1000000000000000000")},
                new BigInteger[]{new BigInteger("1000000000000"), new BigInteger("1")},
                "",
                null,
                contractsHelper,
                Curve2PoolEvent.getSigMap()
        );

        baseStableSwapPool.setStableSwapPoolData(stableSwapPoolData);
        baseStableSwapPool.setReady(true);

        IContractEventWrap ic1 = EventLogUtils.generateContractEvent(
                "txid0",
                new String[]{
                        "2b5508378d7e19e0d5fa338419034731416c4f5b219a10379956f764317fd47e",
                        "0000000000000000000000003802585d6bf577e72284ec4c5e1dc3f0043de734"

                },
                "00000000000000000000000000000000000000000000000000000000002dc6c0000000000000000000000000000000000000000000000001bc16d674ec80000000000000000000000000000000000000000000000000000000000000000001f3000000000000000000000000000000000000000000000000000e6036f6f9a188000000000000000000000000000000000000000000002bdd721ac14c811486f30000000000000000000000000000000000000000000012ef5b23dd8da08c8c9f",
                1678181640000L
        );

        baseStableSwapPool.handleEvent(ic1);
        BigInteger balance0 = new BigInteger("2048048294");
        BigInteger balance1 = new BigInteger("213639313270253951600052");
        BigInteger totalSupply = new BigInteger("89417935861552186690719");
        System.out.println(stableSwapPoolData);
        Assert.isTrue(balance0.compareTo(stableSwapPoolData.getBalances()[0]) == 0, "balance0 not equal");
        Assert.isTrue(balance1.compareTo(stableSwapPoolData.getBalances()[1]) == 0, "balance1 not equal");
        Assert.isTrue(totalSupply.compareTo(stableSwapPoolData.getLpTotalSupply()) == 0, "totalSupply not equal");
    }

    @Test
    public void testRemoveLiquidity() {
        // tx: d6a865e66207cdafe6eab847a5b99babfaa25b5ef1a23a3ea381f53ffa0970b9
        long timestamp = 1678178694L;
        ContractsHelper contractsHelper = new ContractsHelper();
        CurveBasePool curve3Pool = new CurveBasePool(
                "TW6tDjosBGXpjnRMQnHAhPuqTubGs93B9E",
                ContractType.CONTRACT_CURVE_2POOL,
                null,
                null,
                3,
                2,
                "",
                Curve2PoolEvent.getSigMap()
        );
        CurveBasePoolData data = new CurveBasePoolData();
        data.setReady(true);
        data.setAdminFee(BigInteger.valueOf(5000000000L));
        data.setFee(BigInteger.valueOf(4000000));
        data.setBalances(new BigInteger[3]);
        data.updateBalances(0, new BigInteger("588760627928326498224"));
        data.updateBalances(1, new BigInteger("188110934129571857328207"));
        data.updateBalances(2, new BigInteger("851085251952"));
        data.setInitialA(new BigInteger("1500"));
        data.setInitialATime(new BigInteger("0"));
        data.setFutureA(new BigInteger("1500"));
        data.setFutureATime(new BigInteger("0"));
        data.setTotalSupply(new BigInteger("933220588513939137755666"));

        curve3Pool.setCurveBasePoolData(data);
        curve3Pool.setReady(true);

        contractsHelper.getContractMaps().put("TW6tDjosBGXpjnRMQnHAhPuqTubGs93B9E", curve3Pool);
        contractsHelper.setBlockTime(timestamp);

        StableSwapPoolData stableSwapPoolData = new StableSwapPoolData(2, 3);
        stableSwapPoolData.setReady(true);
        stableSwapPoolData.setAdminFee(BigInteger.valueOf(5000000000L));
        stableSwapPoolData.setFee(BigInteger.valueOf(4000000));
        stableSwapPoolData.updateBalances(0, new BigInteger("2048048294"));
        stableSwapPoolData.updateBalances(1, new BigInteger("213639313270253951600052"));
        stableSwapPoolData.setBasePool("TW6tDjosBGXpjnRMQnHAhPuqTubGs93B9E");
        stableSwapPoolData.setBaseCacheUpdated(new BigInteger("1678181640"));
        stableSwapPoolData.setInitialA(new BigInteger("150000"));
        stableSwapPoolData.setInitialATime(new BigInteger("1627548462"));
        stableSwapPoolData.setFutureA(new BigInteger("15000"));
        stableSwapPoolData.setFutureATime(new BigInteger("1627641643"));
        stableSwapPoolData.setBaseVirtualPrice(new BigInteger("1033466048108359893"));
        stableSwapPoolData.setLpTotalSupply(new BigInteger("89417935861552186690719"));

        BaseStableSwapPool baseStableSwapPool = new BaseStableSwapPool(
                "TJ9d9GXCsF7cmnHDLrtT4qo6kNbVK7Qebr",
                ContractType.STABLE_SWAP_POOL,
                3,
                2,
                new BigInteger[]{new BigInteger("1000000000000000000000000000000"), new BigInteger("1000000000000000000")},
                new BigInteger[]{new BigInteger("1000000000000"), new BigInteger("1")},
                "",
                null,
                contractsHelper,
                Curve2PoolEvent.getSigMap()
        );

        baseStableSwapPool.setStableSwapPoolData(stableSwapPoolData);
        baseStableSwapPool.setReady(true);

        IContractEventWrap ic1 = EventLogUtils.generateContractEvent(
                "txid0",
                new String[]{
                        "7c363854ccf79623411f8995b362bce5eddff18c927edc6f5dbbb5e05819a82c",
                        "0000000000000000000000003802585d6bf577e72284ec4c5e1dc3f0043de734"
                },
                "00000000000000000000000000000000000000000000000000000000006e3614000000000000000000000000000000000000000000000028d8095cca7e04261e000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000012de42ce19e8e5603f9b",
                1678178694000L
        );

        baseStableSwapPool.handleEvent(ic1);
        BigInteger balance0 = new BigInteger("2040825490");
        BigInteger balance1 = new BigInteger("212885876431693819355030");
        BigInteger totalSupply = new BigInteger("89102587689556911341467");
        System.out.println(stableSwapPoolData);
        Assert.isTrue(balance0.compareTo(stableSwapPoolData.getBalances()[0]) == 0, "balance0 not equal");
        Assert.isTrue(balance1.compareTo(stableSwapPoolData.getBalances()[1]) == 0, "balance1 not equal");
        Assert.isTrue(totalSupply.compareTo(stableSwapPoolData.getLpTotalSupply()) == 0, "totalSupply not equal");
    }
}
