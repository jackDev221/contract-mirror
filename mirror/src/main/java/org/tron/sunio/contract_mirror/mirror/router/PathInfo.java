package org.tron.sunio.contract_mirror.mirror.router;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PathInfo {
    String contract;
    String tokenName;
    String tokenAddress;
    String poolType;
}
