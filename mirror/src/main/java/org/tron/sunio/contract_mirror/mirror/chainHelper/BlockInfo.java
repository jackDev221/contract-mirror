package org.tron.sunio.contract_mirror.mirror.chainHelper;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class BlockInfo {
    private long number;
    private String hash;
}
