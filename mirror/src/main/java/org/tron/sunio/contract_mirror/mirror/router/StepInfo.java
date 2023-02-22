package org.tron.sunio.contract_mirror.mirror.router;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class StepInfo {
    String contract;
    String tokenName;
    String tokenAddress;
    String poolType;
}
