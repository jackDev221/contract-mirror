package org.tron.sunio.contract_mirror.mirror.contracts.event;

import cn.hutool.core.lang.Assert;
import org.junit.jupiter.api.Test;
import org.tron.sunio.contract_mirror.event_decode.events.SwapV2FactoryEvent;
import org.tron.sunio.contract_mirror.event_decode.events.SwapV2PairEvent;
import org.tron.sunio.contract_mirror.mirror.contracts.events.IContractEventWrap;
import org.tron.sunio.contract_mirror.mirror.contracts.factory.SwapFactoryV2;
import org.tron.sunio.contract_mirror.mirror.contracts.impl.SwapV2Pair;
import org.tron.sunio.contract_mirror.mirror.dao.SwapV2PairData;
import org.tron.sunio.contract_mirror.mirror.utils.ContractsHelper;
import org.tron.sunio.contract_mirror.mirror.utils.EventLogUtils;

import java.math.BigInteger;

public class TestSwapV2 {
    public static String FACTORY_ADDRESS = "THomLGMLhAjMecQf9FQjbZ8a1RtwsZLrGE";
    public static String FACTORY_FEE_TO_ADDRESS = "THomLGMLhAjMecQf9FQjbZ8a1RtwsZLrGE";
    public static String Contract_ADDRESS = "TCTwLZwUWuwc5QtfFMYUKCxauaK9DUnJn2";

    private ContractsHelper getContractsHelp() {
        ContractsHelper contractsHelper = new ContractsHelper();
        SwapFactoryV2 swapFactoryV2 = new SwapFactoryV2(
                FACTORY_ADDRESS,
                null,
                null,
                SwapV2FactoryEvent.getSigMap()
        );
        var data = swapFactoryV2.getSwapFactoryV2Data();
        data.setFeeTo(FACTORY_FEE_TO_ADDRESS);
        swapFactoryV2.setSwapFactoryV2Data(data);
        contractsHelper.getContractMaps().put(swapFactoryV2.getAddress(), swapFactoryV2);
        return contractsHelper;
    }

    @Test
    public void testMint() {
        // txId: 213f6cd999b3d40c776c5087c63bbed5e76a949ed75f52b6d4730eb195ffbe5d
        // init: {"message":"success","code":"success","result":{"address":"TCTwLZwUWuwc5QtfFMYUKCxauaK9DUnJn2","type":"SWAP_V2_PAIR","name":"Uniswap V2","symbol":"UNI-V2","decimals":18,"factory":"THomLGMLhAjMecQf9FQjbZ8a1RtwsZLrGE","token0":"TF17BgPaZYbz8oxbjhriubPDsA7ArKoLX3","token1":"TXYZopYRdj2D9XRtbG411XZZ3kM5VkAeBf","reserve0":11765716889092378956451,"reserve1":33132319422,"blockTimestampLast":1676527446,"price0CumulativeLast":398514543947397876364418834253,"price1CumulativeLast":245666860928863860309119737964453306530744686862692183,"unlocked":0,"trxBalance":0,"lpTotalSupply":19641646134554622,"token0Name":"JUST GOV v1.0","token0Symbol":"JST","token1Name":"Tether USD","token1Symbol":"USDT","reserves":{"reserve0":11765716889092378956451,"reserve1":33132319422,"blockTimestampLast":1676527446},"klast":389825490198228847251005559491322,"ready":true,"addExchangeContracts":false,"using":true}}
        // dest: {"message":"success","code":"success","result":{"address":"TCTwLZwUWuwc5QtfFMYUKCxauaK9DUnJn2","type":"SWAP_V2_PAIR","name":"Uniswap V2","symbol":"UNI-V2","decimals":18,"factory":"THomLGMLhAjMecQf9FQjbZ8a1RtwsZLrGE","token0":"TF17BgPaZYbz8oxbjhriubPDsA7ArKoLX3","token1":"TXYZopYRdj2D9XRtbG411XZZ3kM5VkAeBf","reserve0":11877716889092378956451,"reserve1":33447711999,"blockTimestampLast":1676527995,"price0CumulativeLast":398522571169961548600952585877,"price1CumulativeLast":245667873203635236345015659344243024763792542124813982,"unlocked":0,"trxBalance":0,"lpTotalSupply":19828618537904864,"token0Name":"JUST GOV v1.0","token0Symbol":"JST","token1Name":"Tether USD","token1Symbol":"USDT","klast":397282453712020115841141211155549,"reserves":{"reserve0":11877716889092378956451,"reserve1":33447711999,"blockTimestampLast":1676527995},"ready":true,"addExchangeContracts":false,"using":true}}
        SwapV2Pair swapV2 = new SwapV2Pair(
                FACTORY_ADDRESS,
                Contract_ADDRESS,
                null,
                getContractsHelp(),
                SwapV2PairEvent.getSigMap()
        );

        SwapV2PairData v2PairData = new SwapV2PairData();
        v2PairData.setFactory(FACTORY_ADDRESS);
        v2PairData.setTrxBalance(new BigInteger("0"));
        v2PairData.setLpTotalSupply(new BigInteger("19641646134554622"));
        v2PairData.setBlockTimestampLast(1676527446);
        v2PairData.setReserve0(new BigInteger("11765716889092378956451"));
        v2PairData.setReserve1(new BigInteger("33132319422"));
        v2PairData.setPrice0CumulativeLast(new BigInteger("398514543947397876364418834253"));
        v2PairData.setPrice1CumulativeLast(new BigInteger("245666860928863860309119737964453306530744686862692183"));
        v2PairData.setKLast(new BigInteger("389825490198228847251005559491322"));
        swapV2.setSwapV2PairData(v2PairData);
        System.out.println(v2PairData);
        swapV2.getStateInfo().setReady(true);
        v2PairData.setStateInfo(swapV2.getStateInfo());

        long timeStamp = 1676527995000L;
        IContractEventWrap ic1 = EventLogUtils.generateContractEvent(
                "txid0",
                new String[]{
                        "4c209b5fc8ad50758f13e2e1088ba56a560dff690a1c6fef26394f4c03821c4f",
                        "00000000000000000000000081839e7bccdc7d5f50419bc34209d8ae5969ef66"
                },
                "000000000000000000000000000000000000000000000006124fee993bc000000000000000000000000000000000000000000000000000000000000012cc8241",
                timeStamp
        );
        IContractEventWrap ic2 = EventLogUtils.generateContractEvent(
                "txid1",
                new String[]{
                        "ddf252ad1be2c89b69c2b068fc378daa952ba7f163c4a11628f55a4df523b3ef",
                        "0000000000000000000000000000000000000000000000000000000000000000",
                        "0000000000000000000000003802585d6bf577e72284ec4c5e1dc3f0043de734"
                },
                "0000000000000000000000000000000000000000000000000000aa0ce7af3ae2",
                timeStamp
        );
        swapV2.handleEvent(ic1);
        swapV2.handleEvent(ic2);
        System.out.println(v2PairData);

        long blockTimestampLast = 1676527995;
        BigInteger reserve0 = new BigInteger("11877716889092378956451");
        BigInteger reserve1 = new BigInteger("33447711999");
        BigInteger lpSupply = new BigInteger("19828618537904864");
        BigInteger price0CumulativeLast = new BigInteger("398522571169961548600952585877");
        BigInteger price1CumulativeLast = new BigInteger("245667873203635236345015659344243024763792542124813982");
        BigInteger kLast = new BigInteger("397282453712020115841141211155549");
        Assert.isTrue(v2PairData.getKLast().compareTo(kLast) == 0, "Step1 kLast not equal");
        Assert.isTrue(v2PairData.getReserve0().compareTo(reserve0) == 0, "Step1 reserve0 not equal");
        Assert.isTrue(v2PairData.getReserve1().compareTo(reserve1) == 0, "Step1 reserve1 not equal");
        Assert.isTrue(v2PairData.getLpTotalSupply().compareTo(lpSupply) == 0, "Step1 lpSupply not equal");
        Assert.isTrue(v2PairData.getBlockTimestampLast() == blockTimestampLast, "blockTimestampLast not equal");
        Assert.isTrue(v2PairData.getPrice0CumulativeLast().compareTo(price0CumulativeLast) == 0, "price0CumulativeLast not equal");
        Assert.isTrue(v2PairData.getPrice1CumulativeLast().compareTo(price1CumulativeLast) == 0, "price1CumulativeLast not equal");
    }

    @Test
    public void testBurn() {
        // txId: 95f8e0b950640aa9911c8c5ddd26ced16367044d1ce8f411d5fae1f2d39de5b9
        // init: {"message":"success","code":"success","result":{"address":"TCTwLZwUWuwc5QtfFMYUKCxauaK9DUnJn2","type":"SWAP_V2_PAIR","name":"Uniswap V2","symbol":"UNI-V2","decimals":18,"factory":"THomLGMLhAjMecQf9FQjbZ8a1RtwsZLrGE","token0":"TF17BgPaZYbz8oxbjhriubPDsA7ArKoLX3","token1":"TXYZopYRdj2D9XRtbG411XZZ3kM5VkAeBf","reserve0":11877716889092378956451,"reserve1":33447711999,"blockTimestampLast":1676527995,"price0CumulativeLast":398522571169961548600952585877,"price1CumulativeLast":245667873203635236345015659344243024763792542124813982,"unlocked":0,"trxBalance":0,"lpTotalSupply":19828618537904864,"token0Name":"JUST GOV v1.0","token0Symbol":"JST","token1Name":"Tether USD","token1Symbol":"USDT","klast":397282453712020115841141211155549,"reserves":{"reserve0":11877716889092378956451,"reserve1":33447711999,"blockTimestampLast":1676527995},"ready":true,"addExchangeContracts":false,"using":true}}
        // dest: {"message":"success","code":"success","result":{"address":"TCTwLZwUWuwc5QtfFMYUKCxauaK9DUnJn2","type":"SWAP_V2_PAIR","name":"Uniswap V2","symbol":"UNI-V2","decimals":18,"factory":"THomLGMLhAjMecQf9FQjbZ8a1RtwsZLrGE","token0":"TF17BgPaZYbz8oxbjhriubPDsA7ArKoLX3","token1":"TXYZopYRdj2D9XRtbG411XZZ3kM5VkAeBf","reserve0":11839735098399445161789,"reserve1":33340755081,"blockTimestampLast":1676528367,"price0CumulativeLast":398528010380878935477318128961,"price1CumulativeLast":245668559116595085188219538336661262158690150817510742,"unlocked":0,"trxBalance":0,"lpTotalSupply":19765211870944453,"token0Name":"JUST GOV v1.0","token0Symbol":"JST","token1Name":"Tether USD","token1Symbol":"USDT","reserves":{"reserve0":11839735098399445161789,"reserve1":33340755081,"blockTimestampLast":1676528367},"klast":394745708139655336245497468799909,"ready":true,"addExchangeContracts":false,"using":true}
        SwapV2Pair swapV2 = new SwapV2Pair(
                FACTORY_ADDRESS,
                Contract_ADDRESS,
                null,
                getContractsHelp(),
                SwapV2PairEvent.getSigMap()
        );

        SwapV2PairData v2PairData = new SwapV2PairData();
        v2PairData.setFactory(FACTORY_ADDRESS);
        v2PairData.setTrxBalance(new BigInteger("0"));
        v2PairData.setLpTotalSupply(new BigInteger("19828618537904864"));
        v2PairData.setBlockTimestampLast(1676527995);
        v2PairData.setReserve0(new BigInteger("11877716889092378956451"));
        v2PairData.setReserve1(new BigInteger("33447711999"));
        v2PairData.setPrice0CumulativeLast(new BigInteger("398522571169961548600952585877"));
        v2PairData.setPrice1CumulativeLast(new BigInteger("245667873203635236345015659344243024763792542124813982"));
        v2PairData.setKLast(new BigInteger("397282453712020115841141211155549"));
        swapV2.setSwapV2PairData(v2PairData);
        swapV2.getStateInfo().setReady(true);
        v2PairData.setStateInfo(swapV2.getStateInfo());
        long timeStamp = 1676528367000L;
        IContractEventWrap ic1 = EventLogUtils.generateContractEvent(
                "txid0",
                new String[]{
                        "ddf252ad1be2c89b69c2b068fc378daa952ba7f163c4a11628f55a4df523b3ef",
                        "0000000000000000000000003802585d6bf577e72284ec4c5e1dc3f0043de734",
                        "0000000000000000000000001b5f3d3733582322cef8ba9bf28ef190bb616f60"
                },
                "000000000000000000000000000000000000000000000000000039ab03dc4e1b",
                timeStamp
        );
        IContractEventWrap ic2 = EventLogUtils.generateContractEvent(
                "txid1",
                new String[]{
                        "ddf252ad1be2c89b69c2b068fc378daa952ba7f163c4a11628f55a4df523b3ef",
                        "0000000000000000000000001b5f3d3733582322cef8ba9bf28ef190bb616f60",
                        "0000000000000000000000000000000000000000000000000000000000000000"
                },
                "000000000000000000000000000000000000000000000000000039ab03dc4e1b",
                timeStamp
        );

        IContractEventWrap ic3 = EventLogUtils.generateContractEvent(
                "txid1",
                new String[]{
                        "dccd412f0b1252819cb1fd330b93224ca42612892bb3f4f789976e6d81936496",
                        "00000000000000000000000081839e7bccdc7d5f50419bc34209d8ae5969ef66",
                        "0000000000000000000000003802585d6bf577e72284ec4c5e1dc3f0043de734"
                },
                "0000000000000000000000000000000000000000000000020f1a6d66298e7b660000000000000000000000000000000000000000000000000000000006600876",
                timeStamp
        );
        swapV2.handleEvent(ic1);
        swapV2.handleEvent(ic2);
        swapV2.handleEvent(ic3);

        long blockTimestampLast = 1676528367;
        BigInteger reserve0 = new BigInteger("11839735098399445161789");
        BigInteger reserve1 = new BigInteger("33340755081");
        BigInteger lpSupply = new BigInteger("19765211870944453");
        BigInteger price0CumulativeLast = new BigInteger("398528010380878935477318128961");
        BigInteger price1CumulativeLast = new BigInteger("245668559116595085188219538336661262158690150817510742");
        BigInteger kLast = new BigInteger("394745708139655336245497468799909");
        Assert.isTrue(v2PairData.getKLast().compareTo(kLast) == 0, "Step1 kLast not equal");
        Assert.isTrue(v2PairData.getReserve0().compareTo(reserve0) == 0, "Step1 reserve0 not equal");
        Assert.isTrue(v2PairData.getReserve1().compareTo(reserve1) == 0, "Step1 reserve1 not equal");
        Assert.isTrue(v2PairData.getLpTotalSupply().compareTo(lpSupply) == 0, "Step1 lpSupply not equal");
        Assert.isTrue(v2PairData.getBlockTimestampLast() == blockTimestampLast, "blockTimestampLast not equal");
        Assert.isTrue(v2PairData.getPrice0CumulativeLast().compareTo(price0CumulativeLast) == 0, "price0CumulativeLast not equal");
        Assert.isTrue(v2PairData.getPrice1CumulativeLast().compareTo(price1CumulativeLast) == 0, "price1CumulativeLast not equal");
    }

    @Test
    public void testSwap() {
        // txId: 2d3d7414b90969ed477288378d7bae3e2d89d6c248263cd77203d06f21310bfd
        // init: {"message":"success","code":"success","result":{"address":"TCTwLZwUWuwc5QtfFMYUKCxauaK9DUnJn2","type":"SWAP_V2_PAIR","name":"Uniswap V2","symbol":"UNI-V2","decimals":18,"factory":"THomLGMLhAjMecQf9FQjbZ8a1RtwsZLrGE","token0":"TF17BgPaZYbz8oxbjhriubPDsA7ArKoLX3","token1":"TXYZopYRdj2D9XRtbG411XZZ3kM5VkAeBf","reserve0":11839735098399445161789,"reserve1":33340755081,"blockTimestampLast":1676528367,"price0CumulativeLast":398528010380878935477318128961,"price1CumulativeLast":245668559116595085188219538336661262158690150817510742,"unlocked":0,"trxBalance":0,"lpTotalSupply":19765211870944453,"token0Name":"JUST GOV v1.0","token0Symbol":"JST","token1Name":"Tether USD","token1Symbol":"USDT","reserves":{"reserve0":11839735098399445161789,"reserve1":33340755081,"blockTimestampLast":1676528367},"klast":394745708139655336245497468799909,"ready":true,"addExchangeContracts":false,"using":true}
        // dest: {"message":"success","code":"success","result":{"address":"TCTwLZwUWuwc5QtfFMYUKCxauaK9DUnJn2","type":"SWAP_V2_PAIR","name":"Uniswap V2","symbol":"UNI-V2","decimals":18,"factory":"THomLGMLhAjMecQf9FQjbZ8a1RtwsZLrGE","token0":"TF17BgPaZYbz8oxbjhriubPDsA7ArKoLX3","token1":"TXYZopYRdj2D9XRtbG411XZZ3kM5VkAeBf","reserve0":11950735098399445161789,"reserve1":33032002178,"blockTimestampLast":1676528913,"price0CumulativeLast":398535993738838426078294210299,"price1CumulativeLast":245669565859810334566082110242669863161705408972728504,"unlocked":0,"trxBalance":0,"lpTotalSupply":19765211870944453,"token0Name":"JUST GOV v1.0","token0Symbol":"JST","token1Name":"Tether USD","token1Symbol":"USDT","reserves":{"reserve0":11950735098399445161789,"reserve1":33032002178,"blockTimestampLast":1676528913},"klast":394745708139655336245497468799909,"ready":true,"addExchangeContracts":false,"using":true}
        SwapV2Pair swapV2 = new SwapV2Pair(
                FACTORY_ADDRESS,
                Contract_ADDRESS,
                null,
                getContractsHelp(),
                SwapV2PairEvent.getSigMap()
        );

        SwapV2PairData v2PairData = new SwapV2PairData();
        v2PairData.setFactory(FACTORY_ADDRESS);
        v2PairData.setTrxBalance(new BigInteger("0"));
        v2PairData.setLpTotalSupply(new BigInteger("19765211870944453"));
        v2PairData.setBlockTimestampLast(1676528367);
        v2PairData.setReserve0(new BigInteger("11839735098399445161789"));
        v2PairData.setReserve1(new BigInteger("33340755081"));
        v2PairData.setPrice0CumulativeLast(new BigInteger("398528010380878935477318128961"));
        v2PairData.setPrice1CumulativeLast(new BigInteger("245668559116595085188219538336661262158690150817510742"));
        v2PairData.setKLast(new BigInteger("394745708139655336245497468799909"));
        swapV2.setSwapV2PairData(v2PairData);
        long timeStamp = 1676528913000L;
        swapV2.getStateInfo().setReady(true);
        v2PairData.setStateInfo(swapV2.getStateInfo());
        IContractEventWrap ic1 = EventLogUtils.generateContractEvent(
                "txid0",
                new String[]{
                        "d78ad95fa46c994b6551d0da85fc275fe613ce37657fb8d5e3d130840159d822",
                        "00000000000000000000000081839e7bccdc7d5f50419bc34209d8ae5969ef66",
                        "0000000000000000000000003802585d6bf577e72284ec4c5e1dc3f0043de734"
                },
                "000000000000000000000000000000000000000000000006046f37e5945c0000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000012673207",
                timeStamp
        );

        swapV2.handleEvent(ic1);
        System.out.println(v2PairData);
        long blockTimestampLast = 1676528913;
        BigInteger reserve0 = new BigInteger("11950735098399445161789");
        BigInteger reserve1 = new BigInteger("33032002178");
        BigInteger lpSupply = new BigInteger("19765211870944453");
        BigInteger price0CumulativeLast = new BigInteger("398535993738838426078294210299");
        BigInteger price1CumulativeLast = new BigInteger("245669565859810334566082110242669863161705408972728504");
        BigInteger kLast = new BigInteger("394745708139655336245497468799909");
        Assert.isTrue(v2PairData.getKLast().compareTo(kLast) == 0, "Step1 kLast not equal");
        Assert.isTrue(v2PairData.getReserve0().compareTo(reserve0) == 0, "Step1 reserve0 not equal");
        Assert.isTrue(v2PairData.getReserve1().compareTo(reserve1) == 0, "Step1 reserve1 not equal");
        Assert.isTrue(v2PairData.getLpTotalSupply().compareTo(lpSupply) == 0, "Step1 lpSupply not equal");
        Assert.isTrue(v2PairData.getBlockTimestampLast() == blockTimestampLast, "blockTimestampLast not equal");
        Assert.isTrue(v2PairData.getPrice0CumulativeLast().compareTo(price0CumulativeLast) == 0, "price0CumulativeLast not equal");
        Assert.isTrue(v2PairData.getPrice1CumulativeLast().compareTo(price1CumulativeLast) == 0, "price1CumulativeLast not equal");
    }
}
