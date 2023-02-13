package org.tron.sunio.contract_mirror.mirror.dao;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.tron.sunio.contract_mirror.event_decode.utils.GsonUtil;

import java.math.BigInteger;

@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
public class PSMData extends BaseContractData{
    private String gemJoin;
    private String usdd;
    private String usddJoin;
    private String vat;
    private BigInteger tin;
    private BigInteger tout;
    private String quota;
    private BigInteger[] infos;

    public PSMData copySelf() {
        String jsonString = GsonUtil.objectToGson(this);
        return GsonUtil.gsonToObject(jsonString, PSMData.class);
    }
}
