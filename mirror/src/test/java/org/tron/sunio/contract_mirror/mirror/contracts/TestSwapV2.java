package org.tron.sunio.contract_mirror.mirror.contracts;

import cn.hutool.core.lang.Assert;
import org.junit.jupiter.api.Test;
import org.tron.sunio.contract_mirror.event_decode.events.SwapV2PairEvent;
import org.tron.sunio.contract_mirror.mirror.contracts.events.IContractEventWrap;
import org.tron.sunio.contract_mirror.mirror.contracts.impl.SwapV2Pair;
import org.tron.sunio.contract_mirror.mirror.dao.SwapV2PairData;
import org.tron.sunio.contract_mirror.mirror.utils.EventLogUtils;

import java.math.BigInteger;

public class TestSwapV2 {
    @Test
    public void testSyncDeposit() {
        SwapV2Pair swapV2Pair = new SwapV2Pair(
                "TCTwLZwUWuwc5QtfFMYUKCxauaK9DUnJn2",
                "THomLGMLhAjMecQf9FQjbZ8a1RtwsZLrGE",
                null,
                SwapV2PairEvent.getSigMap()
        );
        swapV2Pair.setReady(true);
        SwapV2PairData swapV2PairData = new SwapV2PairData();
        swapV2PairData.setReady(true);
        swapV2PairData.setTrxBalance(new BigInteger("0"));
        swapV2PairData.setLpTotalSupply(new BigInteger("19601580619932101"));
        swapV2PairData.setBlockTimestampLast(1675846902);
        swapV2PairData.setReserve0(new BigInteger("11741716889092378956451"));
        swapV2PairData.setReserve1(new BigInteger("33064735299"));
        swapV2PairData.setPrice0CumulativeLast(new BigInteger("388563946219729977112471651164"));
        swapV2PairData.setPrice1CumulativeLast(new BigInteger("244412038484425858711638019083106924635981420634717389"));
        swapV2Pair.setSwapV2PairData(swapV2PairData);
        long timeStamp = 1675849749000L;//1675849749
        IContractEventWrap ic1 = EventLogUtils.generateContractEvent(
                "txid0",
                new String[]{
                        "ddf252ad1be2c89b69c2b068fc378daa952ba7f163c4a11628f55a4df523b3ef",
                        "0000000000000000000000000000000000000000000000000000000000000000",
                        "0000000000000000000000003802585d6bf577e72284ec4c5e1dc3f0043de734"
                },
                "000000000000000000000000000000000000000000000000000013bced2db96f",
                timeStamp
        );
        IContractEventWrap ic2 = EventLogUtils.generateContractEvent(
                "txid1",
                new String[]{
                        "1c411e9a96e071241c2f21f7726b17ae89e3cab4c78be50e062b03a9fffbbad1",
                },
                "00000000000000000000000000000000000000000000027d39777b78640192a300000000000000000000000000000000000000000000000000000007b4fe6a86",
                timeStamp
        );
        swapV2Pair.handleEvent(ic1);
        swapV2Pair.handleEvent(ic2);
        BigInteger reserve0 = new BigInteger("11754716889092378956451");
        BigInteger reserve1 = new BigInteger("33101343366");
        long blockTimestampLast = 1675849749L;
        BigInteger price0CumulativeLast = new BigInteger("388605573729091155009886154787");
        BigInteger price1CumulativeLast = new BigInteger("244417287931190964417457149447621755961958779459514602");
        BigInteger lpTotalSupply = new BigInteger("19623282773908276");
        SwapV2PairData res = swapV2Pair.getSwapV2PairData();
        Assert.isTrue(res.getReserve0().compareTo(reserve0) == 0, "reserve0 not equal");
        Assert.isTrue(res.getReserve1().compareTo(reserve1) == 0, "reserve1 not equal");
        Assert.isTrue(res.getLpTotalSupply().compareTo(lpTotalSupply) == 0, "lpSupply not equal");
        Assert.isTrue(res.getBlockTimestampLast() == blockTimestampLast, "blockTimestampLast not equal");
        Assert.isTrue(res.getPrice0CumulativeLast().compareTo(price0CumulativeLast) == 0, "price0CumulativeLast not equal");
        Assert.isTrue(res.getPrice1CumulativeLast().compareTo(price1CumulativeLast) == 0, "price1CumulativeLast not equal");
        ;

    }
}
