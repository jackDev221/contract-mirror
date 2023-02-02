package org.tron.sunio.contract_mirror.mirror.pool.process.out;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SwapV2FactoryExOut extends BaseProcessOut {
    private String token0;
    private String token1;
}
