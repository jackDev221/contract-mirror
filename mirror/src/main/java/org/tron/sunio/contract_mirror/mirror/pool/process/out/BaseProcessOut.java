package org.tron.sunio.contract_mirror.mirror.pool.process.out;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.tron.sunio.contract_mirror.mirror.pool.process.in.BaseProcessIn;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BaseProcessOut {
    protected String outKey;
    protected String address;

    public void initWitBaseIn(BaseProcessIn in) {
        this.outKey = in.getOutKey();
    }
}
