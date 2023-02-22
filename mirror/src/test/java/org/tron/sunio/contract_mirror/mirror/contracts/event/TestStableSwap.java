package org.tron.sunio.contract_mirror.mirror.contracts.event;

import org.junit.jupiter.api.Test;
import org.tron.sunio.contract_mirror.event_decode.events.Curve2PoolEvent;
import org.tron.sunio.contract_mirror.mirror.contracts.events.IContractEventWrap;
import org.tron.sunio.contract_mirror.mirror.contracts.impl.BaseStableSwapPool;
import org.tron.sunio.contract_mirror.mirror.contracts.impl.Curve2Pool;
import org.tron.sunio.contract_mirror.mirror.dao.CurveBasePoolData;
import org.tron.sunio.contract_mirror.mirror.dao.StableSwapPoolData;
import org.tron.sunio.contract_mirror.mirror.enums.ContractType;
import org.tron.sunio.contract_mirror.mirror.utils.ContractsHelper;
import org.tron.sunio.contract_mirror.mirror.utils.EventLogUtils;

import java.math.BigInteger;

public class TestStableSwap {

    @Test
    public void testExchangeUnderlying() {
        long timestamp = 1677048099;
        ContractsHelper contractsHelper = new ContractsHelper();
        Curve2Pool curve2Pool = new Curve2Pool(
                "TTSVAK4j8RKqbQXr3a9eAy1KVjK5Tbnu1W",
                null,
                contractsHelper,
                2,
                1,
                Curve2PoolEvent.getSigMap()
        );
        CurveBasePoolData data = new CurveBasePoolData();
        data.setReady(true);
        data.setAdminFee(BigInteger.valueOf(5000000000L));
        data.setFee(BigInteger.valueOf(4000000));
        data.setBalances(new BigInteger[2]);
        data.updateBalances(0, new BigInteger("12732416934878"));
        data.updateBalances(1, new BigInteger("2080190938224"));
        data.setInitialA(new BigInteger("1000"));
        data.setInitialATime(new BigInteger("0"));
        data.setFutureA(new BigInteger("1000"));
        data.setFutureATime(new BigInteger("0"));
        data.setTotalSupply(new BigInteger("47128390255940168969047"));

        curve2Pool.setCurveBasePoolData(data);
        curve2Pool.setReady(true);
        try{
            curve2Pool.getVirtualPrice(0);
        }catch (Exception e){

        }

        contractsHelper.getContractMaps().put("TTSVAK4j8RKqbQXr3a9eAy1KVjK5Tbnu1W", curve2Pool);
        contractsHelper.setBlockTime(timestamp);

        StableSwapPoolData stableSwapPoolData = new StableSwapPoolData(2, 2);
        stableSwapPoolData.setReady(true);
        stableSwapPoolData.setAdminFee(BigInteger.valueOf(5000000000L));
        stableSwapPoolData.setFee(BigInteger.valueOf(4000000));
        stableSwapPoolData.updateBalances(0, new BigInteger("240087809250511696614785"));
        stableSwapPoolData.updateBalances(1, new BigInteger("563900437235631618614"));
        stableSwapPoolData.setBasePool("TTSVAK4j8RKqbQXr3a9eAy1KVjK5Tbnu1W");
        stableSwapPoolData.setBaseCacheUpdated(new BigInteger("1677046338"));
        stableSwapPoolData.setInitialA(new BigInteger("10000"));
        stableSwapPoolData.setInitialATime(new BigInteger("0"));
        stableSwapPoolData.setFutureA(new BigInteger("10000"));
        stableSwapPoolData.setFutureATime(new BigInteger("0"));
        stableSwapPoolData.setBaseVirtualPrice(new BigInteger("314135400798783964212"));



        BaseStableSwapPool baseStableSwapPool = new BaseStableSwapPool(
                "TUDmvjLmSCqxjzfY3fQWmFRvWzphWQrpQM",
                ContractType.STABLE_SWAP_TUSD,
                2,
                2,
                new BigInteger[]{new BigInteger("1000000000000000000"), new BigInteger("1000000000000000000")},
                new BigInteger[]{new BigInteger("1"), new BigInteger("1")},
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
                "0000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000008ac7230489e8000000000000000000000000000000000000000000000000000000000000000000010000000000000000000000000000000000000000000000000000000000984148",
                1677048099000L
        );

        baseStableSwapPool.handleEvent(ic1);
        System.out.println(stableSwapPoolData);
    }

}
