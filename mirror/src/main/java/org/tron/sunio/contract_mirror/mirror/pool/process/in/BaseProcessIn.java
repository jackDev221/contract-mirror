package org.tron.sunio.contract_mirror.mirror.pool.process.in;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BaseProcessIn {
   private String processType;
   private String outKey;
   private String Address;
   private int id;
}
