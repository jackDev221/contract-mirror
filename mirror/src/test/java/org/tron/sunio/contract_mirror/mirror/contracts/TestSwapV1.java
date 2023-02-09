package org.tron.sunio.contract_mirror.mirror.contracts;

import cn.hutool.core.lang.Assert;
import org.junit.jupiter.api.Test;
import org.tron.sunio.contract_mirror.event_decode.events.SwapV1Event;
import org.tron.sunio.contract_mirror.mirror.contracts.events.IContractEventWrap;
import org.tron.sunio.contract_mirror.mirror.contracts.impl.SwapV1;
import org.tron.sunio.contract_mirror.mirror.dao.SwapV1Data;
import org.tron.sunio.contract_mirror.mirror.utils.EventLogUtils;

import java.math.BigInteger;

public class TestSwapV1 {

    @Test
    public void testSnapShotAndLiquidities() {
        SwapV1 swapV1 = new SwapV1(
                "TFRWUrJ4Yp8zxZn7xvyoVFfhe4iHnhLvuC",
                null,
                null,
                "TQz9i4JygMCzizdVu8NE4BdqesrsHv1L93",
                SwapV1Event.getSigMap()
        );
        swapV1.setReady(true);
        SwapV1Data swapV1Data = new SwapV1Data();
        swapV1Data.setReady(true);
        swapV1Data.setTokenBalance(new BigInteger("809463700822003022937223027"));
        swapV1Data.setTrxBalance(new BigInteger("4314663431319"));
        swapV1Data.setLpTotalSupply(new BigInteger("41614183324"));
        swapV1.setSwapV1Data(swapV1Data);

        // AddLiquidity
        IContractEventWrap log0 = EventLogUtils.generateContractEvent(
                "tx0",
                new String[]{
                        "cc7244d3535e7639366f8c5211527112e01de3ec7449ee3a6e66b007f4065a70",
                        "0000000000000000000000003802585d6bf577e72284ec4c5e1dc3f0043de734",
                        "000000000000000000000000000000000000000000000000000003ec9619b117",
                        "0000000000000000000000000000000000000000029d92af7d4b0fced11396a9"
                },
                ""
        );
        IContractEventWrap log1 = EventLogUtils.generateContractEvent(
                "tx1",
                new String[]{
                        "ddf252ad1be2c89b69c2b068fc378daa952ba7f163c4a11628f55a4df523b3ef",
                        "0000000000000000000000000000000000000000000000000000000000000000",
                        "0000000000000000000000003802585d6bf577e72284ec4c5e1dc3f0043de734"
                },
                "0000000000000000000000000000000000000000000000000000000000004b59"
        );
        swapV1.handleEvent(log0);
        swapV1.handleEvent(log1);
        BigInteger trxBalance = new BigInteger("4314665431319");
        BigInteger tokenBalance = new BigInteger("809464076037226044190856873");
        BigInteger lpSupply = new BigInteger("41614202613");
        Assert.isTrue(swapV1.getSwapV1Data().getTrxBalance().compareTo(trxBalance) == 0, "Step1 trxBalance not equal");
        Assert.isTrue(swapV1.getSwapV1Data().getTokenBalance().compareTo(tokenBalance) == 0, "Step1 tokenBalance not equal");
        Assert.isTrue(swapV1.getSwapV1Data().getLpTotalSupply().compareTo(lpSupply) == 0, "Step1 lpSupply not equal");

        //RemoveLiquidity
        IContractEventWrap log2 = EventLogUtils.generateContractEvent(
                "tx2",
                new String[]{
                        "cc7244d3535e7639366f8c5211527112e01de3ec7449ee3a6e66b007f4065a70",
                        "0000000000000000000000003802585d6bf577e72284ec4c5e1dc3f0043de734",
                        "000000000000000000000000000000000000000000000000000003ec95dbb467",
                        "0000000000000000000000000000000000000000029d92862c8e0ac4c1eff529"
                },
                ""
        );
        IContractEventWrap log3 = EventLogUtils.generateContractEvent(
                "tx3",
                new String[]{
                        "ddf252ad1be2c89b69c2b068fc378daa952ba7f163c4a11628f55a4df523b3ef",
                        "0x0000000000000000000000003802585d6bf577e72284ec4c5e1dc3f0043de734",
                        "0000000000000000000000000000000000000000000000000000000000000000"
                },
                "0x000000000000000000000000000000000000000000000000000000000000990d"
        );
        swapV1.handleEvent(log2);
        swapV1.handleEvent(log3);
        trxBalance = new BigInteger("4314661368935");
        tokenBalance = new BigInteger("809463313902907187704886569");
        lpSupply = new BigInteger("41614163432");
        Assert.isTrue(swapV1.getSwapV1Data().getTrxBalance().compareTo(trxBalance) == 0, "Step2 trxBalance not equal");
        Assert.isTrue(swapV1.getSwapV1Data().getTokenBalance().compareTo(tokenBalance) == 0, "Step2 tokenBalance not equal");
        Assert.isTrue(swapV1.getSwapV1Data().getLpTotalSupply().compareTo(lpSupply) == 0, "Step2 lpSupply not equal");

    }
}
