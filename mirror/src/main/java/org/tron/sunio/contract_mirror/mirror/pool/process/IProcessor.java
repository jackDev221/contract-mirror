package org.tron.sunio.contract_mirror.mirror.pool.process;

import cn.hutool.core.lang.Pair;
import org.tron.sunio.contract_mirror.mirror.pool.process.in.BaseProcessIn;
import org.tron.sunio.contract_mirror.mirror.pool.process.out.BaseProcessOut;

public interface IProcessor {
    Pair<String, BaseProcessOut> getOnlineData(BaseProcessIn in);
}
