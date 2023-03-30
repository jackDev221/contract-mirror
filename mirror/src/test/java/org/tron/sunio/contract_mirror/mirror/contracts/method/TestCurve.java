package org.tron.sunio.contract_mirror.mirror.contracts.method;

import org.junit.jupiter.api.Test;
import org.springframework.util.Assert;
import org.tron.sunio.contract_mirror.event_decode.events.Curve3PoolEvent;
import org.tron.sunio.contract_mirror.mirror.contracts.impl.OldCurvePool;
import org.tron.sunio.contract_mirror.mirror.dao.OldCurvePoolData;
import org.tron.sunio.contract_mirror.mirror.enums.ContractType;
import org.tron.sunio.contract_mirror.mirror.utils.ContractsHelper;

import java.math.BigInteger;

public class TestCurve {

    @Test
    public void testAddLiquidity() {
        // txId: 2f579317accb0f33a21812fddb961165485d1118eeab72021149dde7908fd9b5
        // init: {"message":"success","code":"success","result":{"address":"TAd2UK2c5J4VfMQYxuKL7qqwJsZBocyCfz","type":"CONTRACT_CURVE_3POOL","coins":["TGjgvdTWWrybVLaVeFqSyVqJQWjxqRYbaK","TRz7J6dD2QWxBoumfYt4b3FaiRG23pXfop","TXYZopYRdj2D9XRtbG411XZZ3kM5VkAeBf"],"balance":[237527249623469474950,171670818514848090683,5912730454],"token":"TPbhEF5YDBfKAra7PkcQeJemE55zDTHAyU","fee":4000000,"futureFee":0,"adminFee":5000000000,"futureAdminFee":0,"adminActionsDeadline":0,"feeConverter":"T9yD14Nj9j7xAB4dbGeiX9h8unkKHxuWwb","initialA":400,"initialATime":0,"futureA":400,"futureATime":0,"owner":"TKGRE6oiU3rEzasue4MsB6sCXXSTx9BAe3","futureOwner":"T9yD14Nj9j7xAB4dbGeiX9h8unkKHxuWwb","transferOwnershipDeadline":0,"totalSupply":5804442149820345213230,"ready":true,"addExchangeContracts":false,"using":true}}%
        // dest: {"message":"success","code":"success","result":{"address":"TAd2UK2c5J4VfMQYxuKL7qqwJsZBocyCfz","type":"CONTRACT_CURVE_3POOL","coins":["TGjgvdTWWrybVLaVeFqSyVqJQWjxqRYbaK","TRz7J6dD2QWxBoumfYt4b3FaiRG23pXfop","TXYZopYRdj2D9XRtbG411XZZ3kM5VkAeBf"],"balance":[329521497006893004798,290662722776550821549,6012709393],"token":"TPbhEF5YDBfKAra7PkcQeJemE55zDTHAyU","fee":4000000,"futureFee":0,"adminFee":5000000000,"futureAdminFee":0,"adminActionsDeadline":0,"feeConverter":"T9yD14Nj9j7xAB4dbGeiX9h8unkKHxuWwb","initialA":400,"initialATime":0,"futureA":400,"futureATime":0,"owner":"TKGRE6oiU3rEzasue4MsB6sCXXSTx9BAe3","futureOwner":"T9yD14Nj9j7xAB4dbGeiX9h8unkKHxuWwb","transferOwnershipDeadline":0,"totalSupply":6178218730983441802126,"ready":true,"addExchangeContracts":false,"using":true}}
        ContractsHelper contractsHelper = new ContractsHelper();
        contractsHelper.setBlockTime(1675854858);
        OldCurvePool curve3Pool = new OldCurvePool(
                "TAd2UK2c5J4VfMQYxuKL7qqwJsZBocyCfz",
                ContractType.CONTRACT_CURVE_2POOL,
                "",
                null,
                null,
                3,
                3,
                Curve3PoolEvent.getSigMap()
        );
        OldCurvePoolData data = new OldCurvePoolData();
        data.setAdminFee(BigInteger.valueOf(5000000000L));
        data.setFee(BigInteger.valueOf(4000000));
        data.setBalances(new BigInteger[3]);
        data.updateBalances(0, new BigInteger("237527249623469474950"));
        data.updateBalances(1, new BigInteger("171670818514848090683"));
        data.updateBalances(2, new BigInteger("5912730454"));
        data.setTotalSupply(new BigInteger("5804442149820345213230"));
        data.setInitialA(new BigInteger("400"));
        data.setInitialATime(new BigInteger("0"));
        data.setFutureA(new BigInteger("400"));
        data.setFutureATime(new BigInteger("0"));
        curve3Pool.setOldCurvePoolData(data);
        curve3Pool.getStateInfo().setReady(true);
        data.setStateInfo(curve3Pool.getStateInfo());
        OldCurvePoolData poolData = curve3Pool.getOldCurvePoolData();
        try {

            curve3Pool.addLiquidity(
                    "tx0_0",
                    new BigInteger[]{new BigInteger("92000000000000000000"), new BigInteger("119000000000000000000"), new BigInteger("100000000")},
                    new BigInteger("370053401491792594326"), 1675854858, poolData);
        } catch (Exception e) {
            System.out.println("e" + e.toString());
            e.printStackTrace();
        }
        BigInteger balance0 = new BigInteger("329521497006893004798");
        BigInteger balance1 = new BigInteger("290662722776550821549");
        BigInteger balance2 = new BigInteger("6012709393");
        BigInteger totalSupply = new BigInteger("6178218730983441802126");
        Assert.isTrue(poolData.getBalances()[0].compareTo(balance0) == 0, "balance0 not equal");
        Assert.isTrue(poolData.getBalances()[1].compareTo(balance1) == 0, "balance1 not equal");
        Assert.isTrue(poolData.getBalances()[2].compareTo(balance2) == 0, "balance2 not equal");
        Assert.isTrue(poolData.getTotalSupply().compareTo(totalSupply) == 0, "totalSupply not equal");
    }

    @Test
    public void testExchange() {
        // txId: 02d3c1444f61d2dea52146b4647968df5fe9d778b198cfac62498bcb17938b98
        // init: {"message":"success","code":"success","result":{"address":"TAd2UK2c5J4VfMQYxuKL7qqwJsZBocyCfz","type":"CONTRACT_CURVE_3POOL","coins":["TGjgvdTWWrybVLaVeFqSyVqJQWjxqRYbaK","TRz7J6dD2QWxBoumfYt4b3FaiRG23pXfop","TXYZopYRdj2D9XRtbG411XZZ3kM5VkAeBf"],"balance":[329521497006893004798,290662722776550821549,6012709393],"token":"TPbhEF5YDBfKAra7PkcQeJemE55zDTHAyU","fee":4000000,"futureFee":0,"adminFee":5000000000,"futureAdminFee":0,"adminActionsDeadline":0,"feeConverter":"T9yD14Nj9j7xAB4dbGeiX9h8unkKHxuWwb","initialA":400,"initialATime":0,"futureA":400,"futureATime":0,"owner":"TKGRE6oiU3rEzasue4MsB6sCXXSTx9BAe3","futureOwner":"T9yD14Nj9j7xAB4dbGeiX9h8unkKHxuWwb","transferOwnershipDeadline":0,"totalSupply":6178218730983441802126,"ready":true,"addExchangeContracts":false,"using":true}}
        // dest: {"message":"success","code":"success","result":{"address":"TAd2UK2c5J4VfMQYxuKL7qqwJsZBocyCfz","type":"CONTRACT_CURVE_3POOL","coins":["TGjgvdTWWrybVLaVeFqSyVqJQWjxqRYbaK","TRz7J6dD2QWxBoumfYt4b3FaiRG23pXfop","TXYZopYRdj2D9XRtbG411XZZ3kM5VkAeBf"],"balance":[320183004797908118178,290662722776550821549,6024709393],"token":"TPbhEF5YDBfKAra7PkcQeJemE55zDTHAyU","fee":4000000,"futureFee":0,"adminFee":5000000000,"futureAdminFee":0,"adminActionsDeadline":0,"feeConverter":"T9yD14Nj9j7xAB4dbGeiX9h8unkKHxuWwb","initialA":400,"initialATime":0,"futureA":400,"futureATime":0,"owner":"TKGRE6oiU3rEzasue4MsB6sCXXSTx9BAe3","futureOwner":"T9yD14Nj9j7xAB4dbGeiX9h8unkKHxuWwb","transferOwnershipDeadline":0,"totalSupply":6178218730983441802126,"ready":true,"addExchangeContracts":false,"using":true}}%
        ContractsHelper contractsHelper = new ContractsHelper();
        contractsHelper.setBlockTime(1675826787);
        OldCurvePool curve3Pool = new OldCurvePool(
                "TAd2UK2c5J4VfMQYxuKL7qqwJsZBocyCfz",
                ContractType.CONTRACT_CURVE_2POOL,
                "",
                null,
                null,
                3,
                3,
                Curve3PoolEvent.getSigMap()
        );
        OldCurvePoolData data = new OldCurvePoolData();
        data.setAdminFee(BigInteger.valueOf(5000000000L));
        data.setFee(BigInteger.valueOf(4000000));
        data.setBalances(new BigInteger[3]);
        data.updateBalances(0, new BigInteger("329521497006893004798"));
        data.updateBalances(1, new BigInteger("290662722776550821549"));
        data.updateBalances(2, new BigInteger("6012709393"));
        data.setTotalSupply(new BigInteger("6178218730983441802126"));
        data.setInitialA(new BigInteger("400"));
        data.setInitialATime(new BigInteger("0"));
        data.setFutureA(new BigInteger("400"));
        data.setFutureATime(new BigInteger("0"));
        curve3Pool.setOldCurvePoolData(data);
        curve3Pool.getStateInfo().setReady(true);
        data.setStateInfo(curve3Pool.getStateInfo());
        OldCurvePoolData poolData = curve3Pool.getOldCurvePoolData();
        BigInteger dy = BigInteger.ZERO;
        try {
            dy = curve3Pool.exchange("tx0_0", 2, 0, new BigInteger("12000000"), new BigInteger("9289941016244035010"), 1675826787, poolData);
        } catch (Exception e) {
            System.out.println("e" + e.toString());
            e.printStackTrace();
        }

        BigInteger dyEx = new BigInteger("9336624136928678402");
        BigInteger balance0 = new BigInteger("320183004797908118178");
        BigInteger balance1 = new BigInteger("290662722776550821549");
        BigInteger balance2 = new BigInteger("6024709393");
        System.out.println(poolData);
        BigInteger totalSupply = new BigInteger("6178218730983441802126");
        Assert.isTrue(dyEx.compareTo(dy) == 0, "dy not equal");
        Assert.isTrue(poolData.getBalances()[0].compareTo(balance0) == 0, "balance0 not equal");
        Assert.isTrue(poolData.getBalances()[0].compareTo(balance0) == 0, "balance0 not equal");
        Assert.isTrue(poolData.getBalances()[1].compareTo(balance1) == 0, "balance1 not equal");
        Assert.isTrue(poolData.getBalances()[2].compareTo(balance2) == 0, "balance2 not equal");
        Assert.isTrue(poolData.getTotalSupply().compareTo(totalSupply) == 0, "totalSupply not equal");
    }

    @Test
    public void testRemoveLiquidity() {
        // txID: 2598b6b98e957befd1752b2be89b81f4178309db806cd26740c0bff3f8f7b324
        // init: {"message":"success","code":"success","result":{"address":"TAd2UK2c5J4VfMQYxuKL7qqwJsZBocyCfz","type":"CONTRACT_CURVE_3POOL","coins":["TGjgvdTWWrybVLaVeFqSyVqJQWjxqRYbaK","TRz7J6dD2QWxBoumfYt4b3FaiRG23pXfop","TXYZopYRdj2D9XRtbG411XZZ3kM5VkAeBf"],"balance":[320183004797908118178,290662722776550821549,6024709393],"token":"TPbhEF5YDBfKAra7PkcQeJemE55zDTHAyU","fee":4000000,"futureFee":0,"adminFee":5000000000,"futureAdminFee":0,"adminActionsDeadline":0,"feeConverter":"T9yD14Nj9j7xAB4dbGeiX9h8unkKHxuWwb","initialA":400,"initialATime":0,"futureA":400,"futureATime":0,"owner":"TKGRE6oiU3rEzasue4MsB6sCXXSTx9BAe3","futureOwner":"T9yD14Nj9j7xAB4dbGeiX9h8unkKHxuWwb","transferOwnershipDeadline":0,"totalSupply":6178218730983441802126,"ready":true,"addExchangeContracts":false,"using":true}}
        // dest: {"message":"success","code":"success","result":{"address":"TAd2UK2c5J4VfMQYxuKL7qqwJsZBocyCfz","type":"CONTRACT_CURVE_3POOL","coins":["TGjgvdTWWrybVLaVeFqSyVqJQWjxqRYbaK","TRz7J6dD2QWxBoumfYt4b3FaiRG23pXfop","TXYZopYRdj2D9XRtbG411XZZ3kM5VkAeBf"],"balance":[272544332444876501468,247416248078957405333,5128318416],"token":"TPbhEF5YDBfKAra7PkcQeJemE55zDTHAyU","fee":4000000,"futureFee":0,"adminFee":5000000000,"futureAdminFee":0,"adminActionsDeadline":0,"feeConverter":"T9yD14Nj9j7xAB4dbGeiX9h8unkKHxuWwb","initialA":400,"initialATime":0,"futureA":400,"futureATime":0,"owner":"TKGRE6oiU3rEzasue4MsB6sCXXSTx9BAe3","futureOwner":null,"transferOwnershipDeadline":null,"totalSupply":5258987749200220399191,"ready":false,"using":true,"addExchangeContracts":false}}
        ContractsHelper contractsHelper = new ContractsHelper();
        contractsHelper.setBlockTime(1675827405);
        OldCurvePool curve3Pool = new OldCurvePool(
                "TAd2UK2c5J4VfMQYxuKL7qqwJsZBocyCfz",
                ContractType.CONTRACT_CURVE_2POOL,
                "",
                null,
                null,
                3,
                3,
                Curve3PoolEvent.getSigMap()
        );
        OldCurvePoolData data = new OldCurvePoolData();
        data.setAdminFee(BigInteger.valueOf(5000000000L));
        data.setFee(BigInteger.valueOf(4000000));
        data.setBalances(new BigInteger[3]);
        data.updateBalances(0, new BigInteger("320183004797908118178"));
        data.updateBalances(1, new BigInteger("290662722776550821549"));
        data.updateBalances(2, new BigInteger("6024709393"));
        data.setTotalSupply(new BigInteger("6178218730983441802126"));
        data.setInitialA(new BigInteger("400"));
        data.setInitialATime(new BigInteger("0"));
        data.setFutureA(new BigInteger("400"));
        data.setFutureATime(new BigInteger("0"));
        curve3Pool.setOldCurvePoolData(data);
        curve3Pool.getStateInfo().setReady(true);
        data.setStateInfo(curve3Pool.getStateInfo());
        OldCurvePoolData poolData = curve3Pool.getOldCurvePoolData();
        try {
            curve3Pool.removeLiquidity(
                    new BigInteger("919230981783221402935"),
                    new BigInteger[]{new BigInteger("46685898905970984377"), new BigInteger("42381545203641547892"), new BigInteger("878463158")},
                    poolData);
        } catch (Exception e) {
            System.out.println("e" + e.toString());
            e.printStackTrace();
        }
        BigInteger balance0 = new BigInteger("272544332444876501468");
        BigInteger balance1 = new BigInteger("247416248078957405333");
        BigInteger balance2 = new BigInteger("5128318416");
        BigInteger totalSupply = new BigInteger("5258987749200220399191");
        Assert.isTrue(poolData.getBalances()[0].compareTo(balance0) == 0, "balance0 not equal");
        Assert.isTrue(poolData.getBalances()[1].compareTo(balance1) == 0, "balance1 not equal");
        Assert.isTrue(poolData.getBalances()[2].compareTo(balance2) == 0, "balance2 not equal");
        Assert.isTrue(poolData.getTotalSupply().compareTo(totalSupply) == 0, "totalSupply not equal");
    }

    @Test
    public void TestRemoveLiquidityInBalance() {
        // txID: 9e989aea70b2a688b240abf553a610cfca732bc8e8c2113dfeff7eae32dfc3b4
        // init: {"message":"success","code":"success","result":{"address":"TAd2UK2c5J4VfMQYxuKL7qqwJsZBocyCfz","type":"CONTRACT_CURVE_3POOL","coins":["TGjgvdTWWrybVLaVeFqSyVqJQWjxqRYbaK","TRz7J6dD2QWxBoumfYt4b3FaiRG23pXfop","TXYZopYRdj2D9XRtbG411XZZ3kM5VkAeBf"],"balance":[272544332444876501468,247416248078957405333,5128318416],"token":"TPbhEF5YDBfKAra7PkcQeJemE55zDTHAyU","fee":4000000,"futureFee":0,"adminFee":5000000000,"futureAdminFee":0,"adminActionsDeadline":0,"feeConverter":"T9yD14Nj9j7xAB4dbGeiX9h8unkKHxuWwb","initialA":400,"initialATime":0,"futureA":400,"futureATime":0,"owner":"TKGRE6oiU3rEzasue4MsB6sCXXSTx9BAe3","futureOwner":null,"transferOwnershipDeadline":null,"totalSupply":5258987749200220399191,"ready":false,"using":true,"addExchangeContracts":false}}
        // dest: {"message":"success","code":"success","result":{"address":"TAd2UK2c5J4VfMQYxuKL7qqwJsZBocyCfz","type":"CONTRACT_CURVE_3POOL","coins":["TGjgvdTWWrybVLaVeFqSyVqJQWjxqRYbaK","TRz7J6dD2QWxBoumfYt4b3FaiRG23pXfop","TXYZopYRdj2D9XRtbG411XZZ3kM5VkAeBf"],"balance":[272458429497381821503,247334698938961818280,5127855248],"token":"TPbhEF5YDBfKAra7PkcQeJemE55zDTHAyU","fee":4000000,"futureFee":0,"adminFee":5000000000,"futureAdminFee":0,"adminActionsDeadline":0,"feeConverter":"T9yD14Nj9j7xAB4dbGeiX9h8unkKHxuWwb","initialA":400,"initialATime":0,"futureA":400,"futureATime":0,"owner":"TKGRE6oiU3rEzasue4MsB6sCXXSTx9BAe3","futureOwner":"T9yD14Nj9j7xAB4dbGeiX9h8unkKHxuWwb","transferOwnershipDeadline":0,"totalSupply":5258370013366233062506,"ready":true,"addExchangeContracts":false,"using":true}}
        ContractsHelper contractsHelper = new ContractsHelper();
        contractsHelper.setBlockTime(1675828071);
        OldCurvePool curve3Pool = new OldCurvePool(
                "TAd2UK2c5J4VfMQYxuKL7qqwJsZBocyCfz",
                ContractType.CONTRACT_CURVE_2POOL,
                "",
                null,
                null,
                3,
                3,
                Curve3PoolEvent.getSigMap()
        );

        OldCurvePoolData data = new OldCurvePoolData();
        data.setAdminFee(BigInteger.valueOf(5000000000L));
        data.setFee(BigInteger.valueOf(4000000));
        data.setBalances(new BigInteger[3]);
        data.updateBalances(0, new BigInteger("272544332444876501468"));
        data.updateBalances(1, new BigInteger("247416248078957405333"));
        data.updateBalances(2, new BigInteger("5128318416"));
        data.setTotalSupply(new BigInteger("5258987749200220399191"));
        data.setInitialA(new BigInteger("400"));
        data.setInitialATime(new BigInteger("0"));
        data.setFutureA(new BigInteger("400"));
        data.setFutureATime(new BigInteger("0"));
        curve3Pool.setOldCurvePoolData(data);
        curve3Pool.getStateInfo().setReady(true);
        data.setStateInfo(curve3Pool.getStateInfo());
        OldCurvePoolData poolData = curve3Pool.getOldCurvePoolData();

        try {
            curve3Pool.removeLiquidityImbalance(
                    "tx0_0",
                    new BigInteger[]{new BigInteger("85898905970984377"), new BigInteger("81545203641547892"), new BigInteger("463158")},
                    new BigInteger("4258987749200220399191"),
                    1675828071, poolData);
        } catch (Exception e) {
            System.out.println("e" + e.toString());
            e.printStackTrace();
        }
        BigInteger balance0 = new BigInteger("272458429497381821503");
        BigInteger balance1 = new BigInteger("247334698938961818280");
        BigInteger balance2 = new BigInteger("5127855248");
        BigInteger totalSupply = new BigInteger("5258370013366233062506");
        Assert.isTrue(poolData.getBalances()[0].compareTo(balance0) == 0, "balance0 not equal");
        Assert.isTrue(poolData.getBalances()[1].compareTo(balance1) == 0, "balance1 not equal");
        Assert.isTrue(poolData.getBalances()[2].compareTo(balance2) == 0, "balance2 not equal");
        Assert.isTrue(poolData.getTotalSupply().compareTo(totalSupply) == 0, "totalSupply not equal");
    }

    @Test
    public void TestRemoveLiquidityOneCoin() {
        // txID: 51e14916d582a5fb59cac14ff6ebd959c5153c1d11f654fc58192f5a3cc0af12
        // init: {"message":"success","code":"success","result":{"address":"TAd2UK2c5J4VfMQYxuKL7qqwJsZBocyCfz","type":"CONTRACT_CURVE_3POOL","coins":["TGjgvdTWWrybVLaVeFqSyVqJQWjxqRYbaK","TRz7J6dD2QWxBoumfYt4b3FaiRG23pXfop","TXYZopYRdj2D9XRtbG411XZZ3kM5VkAeBf"],"coinNames":["Decentralized USD","TrueUSD","Tether USD"],"coinSymbols":["USDD","TUSD","USDT"],"balances":[213035218821266858741,270333247145981810421,5171852884],"token":"TPbhEF5YDBfKAra7PkcQeJemE55zDTHAyU","fee":4000000,"futureFee":0,"adminFee":5000000000,"futureAdminFee":0,"adminActionsDeadline":0,"feeConverter":"T9yD14Nj9j7xAB4dbGeiX9h8unkKHxuWwb","initialA":400,"initialATime":0,"futureA":400,"futureATime":0,"owner":"TKGRE6oiU3rEzasue4MsB6sCXXSTx9BAe3","futureOwner":"T9yD14Nj9j7xAB4dbGeiX9h8unkKHxuWwb","transferOwnershipDeadline":0,"totalSupply":5252915340571931328125,"copyBalances":[213035218821266858741,270333247145981810421,5171852884],"ready":true,"addExchangeContracts":false,"using":true}}
        // dest: {"message":"success","code":"success","result":{"address":"TAd2UK2c5J4VfMQYxuKL7qqwJsZBocyCfz","type":"CONTRACT_CURVE_3POOL","coins":["TGjgvdTWWrybVLaVeFqSyVqJQWjxqRYbaK","TRz7J6dD2QWxBoumfYt4b3FaiRG23pXfop","TXYZopYRdj2D9XRtbG411XZZ3kM5VkAeBf"],"coinNames":["Decentralized USD","TrueUSD","Tether USD"],"coinSymbols":["USDD","TUSD","USDT"],"balances":[141326499152419653539,270333247145981810421,5171852884],"token":"TPbhEF5YDBfKAra7PkcQeJemE55zDTHAyU","fee":4000000,"futureFee":0,"adminFee":5000000000,"futureAdminFee":0,"adminActionsDeadline":0,"feeConverter":"T9yD14Nj9j7xAB4dbGeiX9h8unkKHxuWwb","initialA":400,"initialATime":0,"futureA":400,"futureATime":0,"owner":"TKGRE6oiU3rEzasue4MsB6sCXXSTx9BAe3","futureOwner":"T9yD14Nj9j7xAB4dbGeiX9h8unkKHxuWwb","transferOwnershipDeadline":0,"totalSupply":5148471747139268255556,"copyBalances":[141326499152419653539,270333247145981810421,5171852884],"ready":true,"using":true,"addExchangeContracts":false}}
        ContractsHelper contractsHelper = new ContractsHelper();
        contractsHelper.setBlockTime(1675828071);
        OldCurvePool curve3Pool = new OldCurvePool(
                "TAd2UK2c5J4VfMQYxuKL7qqwJsZBocyCfz",
                ContractType.CONTRACT_CURVE_2POOL,
                "",
                null,
                null,
                3,
                3,
                Curve3PoolEvent.getSigMap()
        );

        OldCurvePoolData data = new OldCurvePoolData();
        data.setAdminFee(BigInteger.valueOf(5000000000L));
        data.setFee(BigInteger.valueOf(4000000));
        data.setBalances(new BigInteger[3]);
        data.updateBalances(0, new BigInteger("213035218821266858741"));
        data.updateBalances(1, new BigInteger("270333247145981810421"));
        data.updateBalances(2, new BigInteger("5171852884"));
        data.setTotalSupply(new BigInteger("5252915340571931328125"));
        data.setInitialA(new BigInteger("400"));
        data.setInitialATime(new BigInteger("0"));
        data.setFutureA(new BigInteger("400"));
        data.setFutureATime(new BigInteger("0"));
        curve3Pool.setOldCurvePoolData(data);
        curve3Pool.getStateInfo().setReady(true);
        data.setStateInfo(curve3Pool.getStateInfo());
        OldCurvePoolData poolData = curve3Pool.getOldCurvePoolData();
        try {
            curve3Pool.removeLiquidityOneCoin(
                    "tx0_0",
                    new BigInteger("104443593432663072569"),
                    0,
                    new BigInteger("71545496035121175218"),
                    1675828071,
                    poolData);
        } catch (Exception e) {
            System.out.println("e" + e.toString());
            e.printStackTrace();
        }
        BigInteger balance0 = new BigInteger("141326499152419653539");
        BigInteger balance1 = new BigInteger("270333247145981810421");
        BigInteger balance2 = new BigInteger("5171852884");
        BigInteger totalSupply = new BigInteger("5148471747139268255556");
        Assert.isTrue(poolData.getBalances()[0].compareTo(balance0) == 0, "balance0 not equal");
        Assert.isTrue(poolData.getBalances()[1].compareTo(balance1) == 0, "balance1 not equal");
        Assert.isTrue(poolData.getBalances()[2].compareTo(balance2) == 0, "balance2 not equal");
        Assert.isTrue(poolData.getTotalSupply().compareTo(totalSupply) == 0, "totalSupply not equal");
    }
}
