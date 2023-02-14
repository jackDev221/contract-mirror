package org.tron.sunio.contract_mirror.mirror.contracts.method;

import org.junit.jupiter.api.Test;
import org.springframework.util.Assert;
import org.tron.sunio.contract_mirror.event_decode.events.Curve3PoolEvent;
import org.tron.sunio.contract_mirror.mirror.contracts.impl.Curve3Pool;
import org.tron.sunio.contract_mirror.mirror.dao.CurveBasePoolData;
import org.tron.sunio.contract_mirror.mirror.utils.ContractsHelper;

import java.math.BigInteger;

public class TestCurve {
    @Test
    public void testExchange() {
        // txId: 02d3c1444f61d2dea52146b4647968df5fe9d778b198cfac62498bcb17938b98
        // init:State:"message":"success","code":"success","result":{"address":"TAd2UK2c5J4VfMQYxuKL7qqwJsZBocyCfz","type":"CONTRACT_CURVE_3POOL","coins":["TGjgvdTWWrybVLaVeFqSyVqJQWjxqRYbaK","TRz7J6dD2QWxBoumfYt4b3FaiRG23pXfop","TXYZopYRdj2D9XRtbG411XZZ3kM5VkAeBf"],"balance":[329521497006893004798,290662722776550821549,6012709393],"token":"TPbhEF5YDBfKAra7PkcQeJemE55zDTHAyU","fee":4000000,"futureFee":0,"adminFee":5000000000,"futureAdminFee":0,"adminActionsDeadline":0,"feeConverter":"T9yD14Nj9j7xAB4dbGeiX9h8unkKHxuWwb","initialA":400,"initialATime":0,"futureA":400,"futureATime":0,"owner":"TKGRE6oiU3rEzasue4MsB6sCXXSTx9BAe3","futureOwner":"T9yD14Nj9j7xAB4dbGeiX9h8unkKHxuWwb","transferOwnershipDeadline":0,"totalSupply":6178218730983441802126,"ready":true,"addExchangeContracts":false,"using":true}}
        // dest:{"message":"success","code":"success","result":{"address":"TAd2UK2c5J4VfMQYxuKL7qqwJsZBocyCfz","type":"CONTRACT_CURVE_3POOL","coins":["TGjgvdTWWrybVLaVeFqSyVqJQWjxqRYbaK","TRz7J6dD2QWxBoumfYt4b3FaiRG23pXfop","TXYZopYRdj2D9XRtbG411XZZ3kM5VkAeBf"],"balance":[320183004797908118178,290662722776550821549,6024709393],"token":"TPbhEF5YDBfKAra7PkcQeJemE55zDTHAyU","fee":4000000,"futureFee":0,"adminFee":5000000000,"futureAdminFee":0,"adminActionsDeadline":0,"feeConverter":"T9yD14Nj9j7xAB4dbGeiX9h8unkKHxuWwb","initialA":400,"initialATime":0,"futureA":400,"futureATime":0,"owner":"TKGRE6oiU3rEzasue4MsB6sCXXSTx9BAe3","futureOwner":"T9yD14Nj9j7xAB4dbGeiX9h8unkKHxuWwb","transferOwnershipDeadline":0,"totalSupply":6178218730983441802126,"ready":true,"addExchangeContracts":false,"using":true}}%
        ContractsHelper contractsHelper = new ContractsHelper();
        contractsHelper.setBlockTime(1675826787);
        Curve3Pool curve3Pool = new Curve3Pool(
                "TAd2UK2c5J4VfMQYxuKL7qqwJsZBocyCfz",
                null,
                contractsHelper,
                3,
                2,
                Curve3PoolEvent.getSigMap()
        );
        CurveBasePoolData data = new CurveBasePoolData();
        data.setReady(true);
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
        curve3Pool.setCurveBasePoolData(data);
        curve3Pool.setReady(true);
        CurveBasePoolData curveBasePoolData = curve3Pool.getCurveBasePoolData();
        BigInteger dy = BigInteger.ZERO;
        try {
            dy = curve3Pool.exchange(2, 0, new BigInteger("12000000"), new BigInteger("9289941016244035010"), curveBasePoolData);
        } catch (Exception e) {
            System.out.println("e" + e.toString());
            e.printStackTrace();
        }

        BigInteger dyEx = new BigInteger("9336624136928678402");
        BigInteger balance0 = new BigInteger("320183004797908118178");
        BigInteger balance1 = new BigInteger("290662722776550821549");
        BigInteger balance2 = new BigInteger("6024709393");
        System.out.println(curve3Pool.getCurveBasePoolData());
        BigInteger totalSupply = new BigInteger("6178218730983441802126");
        Assert.isTrue(dyEx.compareTo(dy) == 0, "dy not equal");
        Assert.isTrue(curveBasePoolData.getBalances()[0].compareTo(balance0) == 0, "balance0 not equal");
        Assert.isTrue(curveBasePoolData.getBalances()[0].compareTo(balance0) == 0, "balance0 not equal");
        Assert.isTrue(curveBasePoolData.getBalances()[1].compareTo(balance1) == 0, "balance1 not equal");
        Assert.isTrue(curveBasePoolData.getBalances()[2].compareTo(balance2) == 0, "balance2 not equal");
        Assert.isTrue(curveBasePoolData.getTotalSupply().compareTo(totalSupply) == 0, "totalSupply not equal");
    }
}
