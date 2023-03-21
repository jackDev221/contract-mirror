package org.tron.sunio.contract_mirror.mirror.pool.process.impl;

import cn.hutool.core.lang.Pair;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.tron.sunio.contract_mirror.mirror.pool.process.IProcessor;
import org.tron.sunio.contract_mirror.mirror.pool.process.in.BaseProcessIn;
import org.tron.sunio.contract_mirror.mirror.pool.process.out.BaseProcessOut;

@Slf4j
@Component
public class InvalidProcessor implements IProcessor {
    @Override
    public Pair<String, BaseProcessOut> getOnlineData(BaseProcessIn in) {
        log.error("Invalid process");
        return null;
    }
}
