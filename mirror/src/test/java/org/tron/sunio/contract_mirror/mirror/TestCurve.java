package org.tron.sunio.contract_mirror.mirror;

import org.junit.jupiter.api.Test;
import org.tron.sunio.contract_mirror.mirror.contracts.impl.Curve2Pool;
import org.tron.sunio.contract_mirror.mirror.dao.CurveBasePoolData;

import java.math.BigInteger;
import java.util.Map;


public class TestCurve {
    @Test
    public void testTokenExchange(){
        Curve2Pool curve2Pool = new Curve2Pool(
               "TNTfaTpkdd4AQDeqr8SGG7tgdkdjdhbP5c",
                null,
                2,
                1,
               null
        );

        CurveBasePoolData data =  new CurveBasePoolData();
        data.setAdminFee(BigInteger.valueOf(5000000000L));
        data.setFee(BigInteger.valueOf(4000000));
        data.setBalance(new BigInteger[2]);
        data.updateBalances(0, new BigInteger("33925905500171617637391966"));
        data.updateBalances(1, new BigInteger("26817553156304"));
        curve2Pool.setCurveBasePoolData(data);

        curve2Pool.handleEventTokenExchange(
                new String[]{
                        "8b3e96f2b889fa771c53c981b40daf005f63f637f1869f707052d15a3dd97140",
                        "000000000000000000000041a2c2426d23bb43809e6eba1311afddde8d45f5d8"
                },
                "00000000000000000000000000000000000000000000000000000000000000010000000000000000000000000000000000000000000000000000000000d59f800000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000c4034eeb03ef1754",
                null
        );

        curve2Pool.handleEventTokenExchange(
                new String[]{
                        "8b3e96f2b889fa771c53c981b40daf005f63f637f1869f707052d15a3dd97140",
                        "000000000000000000000041a2c2426d23bb43809e6eba1311afddde8d45f5d8"
                },
                "00000000000000000000000000000000000000000000000000000000000000010000000000000000000000000000000000000000000000000000000005f5e10000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000057817930f78c408f4",
                null
        );

        curve2Pool.handleEventTokenExchange(
                new String[]{
                        "8b3e96f2b889fa771c53c981b40daf005f63f637f1869f707052d15a3dd97140",
                        "000000000000000000000041a2c2426d23bb43809e6eba1311afddde8d45f5d8"
                },
                "00000000000000000000000000000000000000000000000000000000000000010000000000000000000000000000000000000000000000000000000002faf0800000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000002bc0bc016734cff7e",
                null
        );
        System.out.println(curve2Pool.getCurveBasePoolData());
    }
}
