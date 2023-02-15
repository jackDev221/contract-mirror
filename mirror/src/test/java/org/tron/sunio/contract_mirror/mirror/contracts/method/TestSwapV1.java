package org.tron.sunio.contract_mirror.mirror.contracts.method;

import cn.hutool.core.lang.Assert;
import org.junit.jupiter.api.Test;
import org.tron.sunio.contract_mirror.event_decode.events.SwapV1Event;
import org.tron.sunio.contract_mirror.event_decode.events.SwapV1FactoryEvent;
import org.tron.sunio.contract_mirror.mirror.contracts.factory.SwapFactoryV1;
import org.tron.sunio.contract_mirror.mirror.contracts.impl.SwapV1;
import org.tron.sunio.contract_mirror.mirror.dao.SwapV1Data;
import org.tron.sunio.contract_mirror.mirror.utils.ContractsHelper;
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
        swapV1Data.setKLast(BigInteger.ZERO);
        swapV1.setSwapV1Data(swapV1Data);

        try {
            swapV1.addLiquidity(
                    new BigInteger("0000000000000000000000000000000000000000000000000000000000b71b00", 16),
                    new BigInteger("000000000000000000000000000000000000000000000000000000000001c1d7", 16),
                    new BigInteger("00000000000000000000000000000000000000000000007aa724c7792dff9d28", 16),
                    swapV1Data
            );
        }catch (Exception e){
            System.out.println(e);
        }
        BigInteger trxBalance = new BigInteger("4314674368935");
        BigInteger tokenBalance = new BigInteger("809465378148457871205899353");
        BigInteger lpSupply = new BigInteger("41614279169");
        System.out.println(swapV1Data);
        Assert.isTrue(swapV1Data.getTrxBalance().compareTo(trxBalance) == 0, "Step1 trxBalance not equal");
        Assert.isTrue(swapV1Data.getTokenBalance().compareTo(tokenBalance) == 0, "Step1 tokenBalance not equal");
        Assert.isTrue(swapV1Data.getLpTotalSupply().compareTo(lpSupply) == 0, "Step1 lpSupply not equal");
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
        swapV1Data.setKLast(BigInteger.ZERO);
        swapV1.setSwapV1Data(swapV1Data);

        try{
            swapV1.removeLiquidity(
                    new BigInteger("0000000000000000000000000000000000000000000000000000000000004eab", 16),
                    new BigInteger("0000000000000000000000000000000000000000000000000000000000000001", 16),
                    new BigInteger("0000000000000000000000000000000000000000000000000000000000000001", 16),
                    swapV1Data
            );
        }catch (Exception e){
            System.out.println(e);
        }
        BigInteger trxBalance = new BigInteger("4314672280873");
        BigInteger tokenBalance = new BigInteger("809464986412169845445309056");
        BigInteger lpSupply = new BigInteger("41614259030");
        System.out.println(swapV1Data);
        Assert.isTrue(swapV1Data.getTrxBalance().compareTo(trxBalance) == 0, "Step1 trxBalance not equal");
        Assert.isTrue(swapV1Data.getTokenBalance().compareTo(tokenBalance) == 0, "Step1 tokenBalance not equal");
        Assert.isTrue(swapV1Data.getLpTotalSupply().compareTo(lpSupply) == 0, "Step1 lpSupply not equal");
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
        try{
            swapV1.trxToTokenInput(
                    new BigInteger("00000000000000000000000000000000000000000000000000000000002dc6c0", 16),
                    new BigInteger("00000000000000000000000000000000000000000000001e445bb22de57b2c5e", 16),
                    swapV1Data
            );
        }catch (Exception e){
            System.out.println(e);
        }
        BigInteger trxBalance = new BigInteger("4314675280873");
        BigInteger tokenBalance = new BigInteger("809464425278452519877826622");
        BigInteger lpSupply = new BigInteger("41614259030");

        Assert.isTrue(swapV1Data.getTrxBalance().compareTo(trxBalance) == 0, "Step1 trxBalance not equal");
        Assert.isTrue(swapV1Data.getTokenBalance().compareTo(tokenBalance) == 0, "Step1 tokenBalance not equal");
        Assert.isTrue(swapV1Data.getLpTotalSupply().compareTo(lpSupply) == 0, "Step1 lpSupply not equal");
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

        try{
            swapV1.tokenToTrxInput(
                    new BigInteger("00000000000000000000000000000000000000000000001043561a8829300000", 16),
                    new BigInteger("000000000000000000000000000000000000000000000000000000000018348b", 16),
                    swapV1Data
            );
        }catch (Exception e){
            System.out.println(e);
        }

        BigInteger trxBalance = new BigInteger("4314673686586");
        BigInteger tokenBalance = new BigInteger("809464725278452519877826622");
        BigInteger lpSupply = new BigInteger("41614259030");
        System.out.println(swapV1Data);
        Assert.isTrue(swapV1Data.getTrxBalance().compareTo(trxBalance) == 0, "Step1 trxBalance not equal");
        Assert.isTrue(swapV1Data.getTokenBalance().compareTo(tokenBalance) == 0, "Step1 tokenBalance not equal");
        Assert.isTrue(swapV1Data.getLpTotalSupply().compareTo(lpSupply) == 0, "Step1 lpSupply not equal");
    }
}
