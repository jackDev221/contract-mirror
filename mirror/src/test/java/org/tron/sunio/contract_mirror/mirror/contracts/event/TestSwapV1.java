package org.tron.sunio.contract_mirror.mirror.contracts.event;

import cn.hutool.core.lang.Assert;
import org.junit.jupiter.api.Test;
import org.tron.sunio.contract_mirror.event_decode.events.SwapV1Event;
import org.tron.sunio.contract_mirror.event_decode.events.SwapV1FactoryEvent;
import org.tron.sunio.contract_mirror.mirror.contracts.events.IContractEventWrap;
import org.tron.sunio.contract_mirror.mirror.contracts.factory.SwapFactoryV1;
import org.tron.sunio.contract_mirror.mirror.contracts.impl.SwapV1;
import org.tron.sunio.contract_mirror.mirror.dao.SwapV1Data;
import org.tron.sunio.contract_mirror.mirror.utils.ContractsHelper;
import org.tron.sunio.contract_mirror.mirror.utils.EventLogUtils;

import java.math.BigInteger;

public class TestSwapV1 {
    public static String FACTORY_ADDRESS = "TXFouUxm4Qs3c1VxfQtCo4xMxbpwE3aWDM";
    public static String Contract_ADDRESS = "TFRWUrJ4Yp8zxZn7xvyoVFfhe4iHnhLvuC";
    public static String TOKEN_ADDRESS = "TQz9i4JygMCzizdVu8NE4BdqesrsHv1L93";

    private ContractsHelper getContractsHelp() {
        ContractsHelper contractsHelper = new ContractsHelper();
        SwapFactoryV1 swapFactoryV1 = new SwapFactoryV1(
                FACTORY_ADDRESS,
                null,
                null,
                SwapV1FactoryEvent.getSigMap()
        );
        var data = swapFactoryV1.getSwapFactoryV1Data();
        data.setFeeTo(FACTORY_ADDRESS);
        swapFactoryV1.setSwapFactoryV1Data(data);
        contractsHelper.getContractMaps().put(swapFactoryV1.getAddress(), swapFactoryV1);
        return contractsHelper;
    }

    @Test
    public void testAddLiquidity() {
        // txId: e4e608c44d45c2ef52f484c26ab6faf0812185bcbccbb9733cf0b4f965b980b7
        // init: {"message":"success","code":"success","result":{"address":"TFRWUrJ4Yp8zxZn7xvyoVFfhe4iHnhLvuC","type":"SWAP_V1","factory":"TXFouUxm4Qs3c1VxfQtCo4xMxbpwE3aWDM","tokenAddress":"TQz9i4JygMCzizdVu8NE4BdqesrsHv1L93","name":"Justswap V1","symbol":"JUSTSWAP-V1","decimals":6,"trxBalance":4314662368935,"tokenBalance":809463126858161732561578006,"lpTotalSupply":41614163432,"tokenName":"Ethereum","tokenSymbol":"ETH","klast":0,"ready":true,"addExchangeContracts":false,"using":true}}
        // dest: {"message":"success","code":"success","result":{"address":"TFRWUrJ4Yp8zxZn7xvyoVFfhe4iHnhLvuC","type":"SWAP_V1","factory":"TXFouUxm4Qs3c1VxfQtCo4xMxbpwE3aWDM","tokenAddress":"TQz9i4JygMCzizdVu8NE4BdqesrsHv1L93","name":"Justswap V1","symbol":"JUSTSWAP-V1","decimals":6,"trxBalance":4314674368935,"tokenBalance":809465378148457871205899353,"lpTotalSupply":41614279169,"tokenName":"Ethereum","tokenSymbol":"ETH","klast":0,"ready":true,"addExchangeContracts":false,"using":true}}%
        SwapV1 swapV1 = new SwapV1(
                FACTORY_ADDRESS,
                Contract_ADDRESS,
                null,
                getContractsHelp(),
                TOKEN_ADDRESS,
                SwapV1Event.getSigMap()
        );

        swapV1.setReady(true);
        SwapV1Data swapV1Data = new SwapV1Data();
        swapV1Data.setReady(true);
        swapV1Data.setFactory(FACTORY_ADDRESS);
        swapV1Data.setTrxBalance(new BigInteger("4314662368935"));
        swapV1Data.setTokenBalance(new BigInteger("809463126858161732561578006"));
        swapV1Data.setLpTotalSupply(new BigInteger("41614163432"));
        swapV1.setSwapV1Data(swapV1Data);
        // AddLiquidity
        // addliquidityEvent
        IContractEventWrap log0 = EventLogUtils.generateContractEvent(
                "tx0",
                new String[]{
                        "06239653922ac7bea6aa2b19dc486b9361821d37712eb796adfd38d81de278ca",
                        "0000000000000000000000003802585d6bf577e72284ec4c5e1dc3f0043de734",
                        "0000000000000000000000000000000000000000000000000000000000b71b00",
                        "00000000000000000000000000000000000000000000007a0aedd47dabdbf443"
                },
                ""
        );
        // trans event
        IContractEventWrap log1 = EventLogUtils.generateContractEvent(
                "tx1",
                new String[]{
                        "ddf252ad1be2c89b69c2b068fc378daa952ba7f163c4a11628f55a4df523b3ef",
                        "0000000000000000000000000000000000000000000000000000000000000000",
                        "0000000000000000000000003802585d6bf577e72284ec4c5e1dc3f0043de734"
                },
                "000000000000000000000000000000000000000000000000000000000001c419"
        );
        swapV1.handleEvent(log0);
        swapV1.handleEvent(log1);
        BigInteger trxBalance = new BigInteger("4314674368935");
        BigInteger tokenBalance = new BigInteger("809465378148457871205899353");
        BigInteger lpSupply = new BigInteger("41614279169");
        SwapV1Data swapV1Data1 = swapV1.getSwapV1Data();
        System.out.println(swapV1Data1);
        Assert.isTrue(swapV1Data1.getTrxBalance().compareTo(trxBalance) == 0, "Step1 trxBalance not equal");
        Assert.isTrue(swapV1Data1.getTokenBalance().compareTo(tokenBalance) == 0, "Step1 tokenBalance not equal");
        Assert.isTrue(swapV1Data1.getLpTotalSupply().compareTo(lpSupply) == 0, "Step1 lpSupply not equal");
    }

    @Test
    public void testRemoveLiquidity() {
        // txId: a325e09026a25809b40f74bc88a77b35e669ff4d2e719e229da4b30e6f7672c6
        // init: {"message":"success","code":"success","result":{"address":"TFRWUrJ4Yp8zxZn7xvyoVFfhe4iHnhLvuC","type":"SWAP_V1","factory":"TXFouUxm4Qs3c1VxfQtCo4xMxbpwE3aWDM","tokenAddress":"TQz9i4JygMCzizdVu8NE4BdqesrsHv1L93","name":"Justswap V1","symbol":"JUSTSWAP-V1","decimals":6,"trxBalance":4314674368935,"tokenBalance":809465378148457871205899353,"lpTotalSupply":41614279169,"tokenName":"Ethereum","tokenSymbol":"ETH","klast":0,"ready":true,"addExchangeContracts":false,"using":true}}%
        // dest: {"message":"success","code":"success","result":{"address":"TFRWUrJ4Yp8zxZn7xvyoVFfhe4iHnhLvuC","type":"SWAP_V1","factory":"TXFouUxm4Qs3c1VxfQtCo4xMxbpwE3aWDM","tokenAddress":"TQz9i4JygMCzizdVu8NE4BdqesrsHv1L93","name":"Justswap V1","symbol":"JUSTSWAP-V1","decimals":6,"trxBalance":4314672280873,"tokenBalance":809464986412169845445309056,"lpTotalSupply":41614259030,"tokenName":"Ethereum","tokenSymbol":"ETH","klast":0,"ready":true,"addExchangeContracts":false,"using":true}}
        SwapV1 swapV1 = new SwapV1(
                FACTORY_ADDRESS,
                Contract_ADDRESS,
                null,
                getContractsHelp(),
                TOKEN_ADDRESS,
                SwapV1Event.getSigMap()
        );

        swapV1.setReady(true);
        SwapV1Data swapV1Data = new SwapV1Data();
        swapV1Data.setReady(true);
        swapV1Data.setFactory(FACTORY_ADDRESS);
        swapV1Data.setTrxBalance(new BigInteger("4314674368935"));
        swapV1Data.setTokenBalance(new BigInteger("809465378148457871205899353"));
        swapV1Data.setLpTotalSupply(new BigInteger("41614279169"));
        swapV1.setSwapV1Data(swapV1Data);
        // RmLiquidity
        // rmliquidityEvent
        IContractEventWrap log0 = EventLogUtils.generateContractEvent(
                "tx0",
                new String[]{
                        "0fbf06c058b90cb038a618f8c2acbf6145f8b3570fd1fa56abb8f0f3f05b36e8",
                        "0000000000000000000000003802585d6bf577e72284ec4c5e1dc3f0043de734",
                        "00000000000000000000000000000000000000000000000000000000001fdc7e",
                        "0000000000000000000000000000000000000000000000153c6ede739d9169d9"
                },
                ""
        );
        // trans event
        IContractEventWrap log1 = EventLogUtils.generateContractEvent(
                "tx1",
                new String[]{
                        "ddf252ad1be2c89b69c2b068fc378daa952ba7f163c4a11628f55a4df523b3ef",
                        "0000000000000000000000003802585d6bf577e72284ec4c5e1dc3f0043de734",
                        "0000000000000000000000000000000000000000000000000000000000000000"
                },
                "0000000000000000000000000000000000000000000000000000000000004eab"
        );
        swapV1.handleEvent(log0);
        swapV1.handleEvent(log1);
        BigInteger trxBalance = new BigInteger("4314672280873");
        BigInteger tokenBalance = new BigInteger("809464986412169845445309056");
        BigInteger lpSupply = new BigInteger("41614259030");
        SwapV1Data swapV1Data1 = swapV1.getSwapV1Data();
        System.out.println(swapV1Data1);
        Assert.isTrue(swapV1Data1.getTrxBalance().compareTo(trxBalance) == 0, "Step1 trxBalance not equal");
        Assert.isTrue(swapV1Data1.getTokenBalance().compareTo(tokenBalance) == 0, "Step1 tokenBalance not equal");
        Assert.isTrue(swapV1Data1.getLpTotalSupply().compareTo(lpSupply) == 0, "Step1 lpSupply not equal");
    }

    @Test
    public void testTokenPurchase() {
        // txId: b108ad0507e2060047e5e3d46609d06e4bf8a3c32deaf81d96cbadce57c72874
        // init: {"message":"success","code":"success","result":{"address":"TFRWUrJ4Yp8zxZn7xvyoVFfhe4iHnhLvuC","type":"SWAP_V1","factory":"TXFouUxm4Qs3c1VxfQtCo4xMxbpwE3aWDM","tokenAddress":"TQz9i4JygMCzizdVu8NE4BdqesrsHv1L93","name":"Justswap V1","symbol":"JUSTSWAP-V1","decimals":6,"trxBalance":4314672280873,"tokenBalance":809464986412169845445309056,"lpTotalSupply":41614259030,"tokenName":"Ethereum","tokenSymbol":"ETH","klast":0,"ready":true,"addExchangeContracts":false,"using":true}}
        // dest: {"message":"success","code":"success","result":{"address":"TFRWUrJ4Yp8zxZn7xvyoVFfhe4iHnhLvuC","type":"SWAP_V1","factory":"TXFouUxm4Qs3c1VxfQtCo4xMxbpwE3aWDM","tokenAddress":"TQz9i4JygMCzizdVu8NE4BdqesrsHv1L93","name":"Justswap V1","symbol":"JUSTSWAP-V1","decimals":6,"trxBalance":4314675280873,"tokenBalance":809464425278452519877826622,"lpTotalSupply":41614259030,"tokenName":"Ethereum","tokenSymbol":"ETH","klast":0,"ready":true,"addExchangeContracts":false,"using":true}}
        SwapV1 swapV1 = new SwapV1(
                FACTORY_ADDRESS,
                Contract_ADDRESS,
                null,
                getContractsHelp(),
                TOKEN_ADDRESS,
                SwapV1Event.getSigMap()
        );

        swapV1.setReady(true);
        SwapV1Data swapV1Data = new SwapV1Data();
        swapV1Data.setReady(true);
        swapV1Data.setFactory(FACTORY_ADDRESS);
        swapV1Data.setTrxBalance(new BigInteger("4314672280873"));
        swapV1Data.setTokenBalance(new BigInteger("809464986412169845445309056"));
        swapV1Data.setLpTotalSupply(new BigInteger("41614259030"));
        swapV1.setSwapV1Data(swapV1Data);

        // tokenPurchase
        IContractEventWrap log0 = EventLogUtils.generateContractEvent(
                "tx0",
                new String[]{
                        "cd60aa75dea3072fbc07ae6d7d856b5dc5f4eee88854f5b4abf7b680ef8bc50f",
                        "0000000000000000000000003802585d6bf577e72284ec4c5e1dc3f0043de734",
                        "00000000000000000000000000000000000000000000000000000000002dc6c0",
                        "00000000000000000000000000000000000000000000001e6b4b6ee4cce7da42"
                },
                ""
        );

        swapV1.handleEvent(log0);
        BigInteger trxBalance = new BigInteger("4314675280873");
        BigInteger tokenBalance = new BigInteger("809464425278452519877826622");
        BigInteger lpSupply = new BigInteger("41614259030");
        SwapV1Data swapV1Data1 = swapV1.getSwapV1Data();
        System.out.println(swapV1Data1);
        Assert.isTrue(swapV1Data1.getTrxBalance().compareTo(trxBalance) == 0, "Step1 trxBalance not equal");
        Assert.isTrue(swapV1Data1.getTokenBalance().compareTo(tokenBalance) == 0, "Step1 tokenBalance not equal");
        Assert.isTrue(swapV1Data1.getLpTotalSupply().compareTo(lpSupply) == 0, "Step1 lpSupply not equal");
    }

    @Test
    public void testTrxPurchase() {
        // txId: c481460536d1357368c02bddf1433f62950c30d0fdd4b5df67434a838125c054
        // init: {"message":"success","code":"success","result":{"address":"TFRWUrJ4Yp8zxZn7xvyoVFfhe4iHnhLvuC","type":"SWAP_V1","factory":"TXFouUxm4Qs3c1VxfQtCo4xMxbpwE3aWDM","tokenAddress":"TQz9i4JygMCzizdVu8NE4BdqesrsHv1L93","name":"Justswap V1","symbol":"JUSTSWAP-V1","decimals":6,"trxBalance":4314675280873,"tokenBalance":809464425278452519877826622,"lpTotalSupply":41614259030,"tokenName":"Ethereum","tokenSymbol":"ETH","klast":0,"ready":true,"addExchangeContracts":false,"using":true}}
        // dest:{"message":"success","code":"success","result":{"address":"TFRWUrJ4Yp8zxZn7xvyoVFfhe4iHnhLvuC","type":"SWAP_V1","factory":"TXFouUxm4Qs3c1VxfQtCo4xMxbpwE3aWDM","tokenAddress":"TQz9i4JygMCzizdVu8NE4BdqesrsHv1L93","name":"Justswap V1","symbol":"JUSTSWAP-V1","decimals":6,"trxBalance":4314673686586,"tokenBalance":809464725278452519877826622,"lpTotalSupply":41614259030,"tokenName":"Ethereum","tokenSymbol":"ETH","klast":0,"ready":true,"addExchangeContracts":false,"using":true}}
        SwapV1 swapV1 = new SwapV1(
                FACTORY_ADDRESS,
                Contract_ADDRESS,
                null,
                getContractsHelp(),
                TOKEN_ADDRESS,
                SwapV1Event.getSigMap()
        );

        swapV1.setReady(true);
        SwapV1Data swapV1Data = new SwapV1Data();
        swapV1Data.setReady(true);
        swapV1Data.setFactory(FACTORY_ADDRESS);
        swapV1Data.setTrxBalance(new BigInteger("4314675280873"));
        swapV1Data.setTokenBalance(new BigInteger("809464425278452519877826622"));
        swapV1Data.setLpTotalSupply(new BigInteger("41614259030"));
        swapV1.setSwapV1Data(swapV1Data);

        // TxPurchase
        IContractEventWrap log0 = EventLogUtils.generateContractEvent(
                "tx0",
                new String[]{
                        "dad9ec5c9b9c82bf6927bf0b64293dcdd1f82c92793aef3c5f26d7b93a4a5306",
                        "0000000000000000000000003802585d6bf577e72284ec4c5e1dc3f0043de734",
                        "00000000000000000000000000000000000000000000001043561a8829300000",
                        "00000000000000000000000000000000000000000000000000000000001853af"

                },
                ""
        );

        swapV1.handleEvent(log0);
        BigInteger trxBalance = new BigInteger("4314673686586");
        BigInteger tokenBalance = new BigInteger("809464725278452519877826622");
        BigInteger lpSupply = new BigInteger("41614259030");
        SwapV1Data swapV1Data1 = swapV1.getSwapV1Data();
        System.out.println(swapV1Data1);
        Assert.isTrue(swapV1Data1.getTrxBalance().compareTo(trxBalance) == 0, "Step1 trxBalance not equal");
        Assert.isTrue(swapV1Data1.getTokenBalance().compareTo(tokenBalance) == 0, "Step1 tokenBalance not equal");
        Assert.isTrue(swapV1Data1.getLpTotalSupply().compareTo(lpSupply) == 0, "Step1 lpSupply not equal");
    }

}
