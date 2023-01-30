package org.tron.sunio.contract_mirror.mirror.pool;

import cn.hutool.core.util.ObjectUtil;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.tron.sunio.contract_mirror.mirror.pool.process.IProcessor;
import org.tron.sunio.contract_mirror.mirror.pool.process.in.BaseProcessIn;
import org.tron.sunio.contract_mirror.mirror.pool.process.out.BaseProcessOut;
import org.tron.sunio.contract_mirror.mirror.tools.TimeTool;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Slf4j
@Component
public class CMPool {

    @Autowired
    private ProcessInitializer initializer;

    private ExecutorService fixedThreadPool = Executors.newFixedThreadPool(10);

    private List<BaseProcessOut> res = new ArrayList<>();

    @Getter
    private ConcurrentHashMap<String, BaseProcessOut> resMap = new ConcurrentHashMap<>();

    @Setter
    CountDownLatch latch;

    public void submit(BaseProcessIn in) {
        IProcessor iProcessor = initializer.getProcessor(in.getProcessType());
        fixedThreadPool.submit(() -> {
            int flag = 20;
            while (flag > 0) {
                flag--;
                try {
                    var res = iProcessor.getOnlineData(in);
                    if (ObjectUtil.isNotNull(res)) {
                        resMap.put(res.getKey(), res.getValue());
                        break;
                    }
                } catch (Exception e) {
                    TimeTool.sleep(10);
                    log.error(e.getStackTrace().toString());
                }
            }
            if (ObjectUtil.isNotNull(latch)) {
                latch.countDown();
            }

        });
    }

    public void submitT(Runnable r){
        fixedThreadPool.submit(r);
    }

    public CountDownLatch initCountDownLatch(int count) {
        latch = new CountDownLatch(count);
        resMap.clear();
        return  latch;
    }

    public void waitFinish() {
        try {
            latch.await();
//            fixedThreadPool.shutdown();
        } catch (Exception e) {
            log.error(e.getStackTrace().toString());
        }
    }

    public BaseProcessOut getProcessRes(String key) {
        return this.resMap.get(key);
    }

    public int getResultSize() {
        return this.resMap.size();
    }
}
