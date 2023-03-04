package org.tron.defi.contract_mirror.utils;


import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.tron.defi.contract_mirror.config.ServerConfig;

import java.util.ArrayList;

@Slf4j
public class ShardedThreadPool {
    private final ArrayList<ThreadPoolTaskExecutor> executors;

    public ShardedThreadPool(ServerConfig.ThreadPoolConfig threadPoolConfig) {
        int threadNum = Math.max(1, threadPoolConfig.getThreadNum());
        executors = new ArrayList<>(threadNum);
        for (int i = 0; i < threadNum; ++i) {
            ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
            executor.setCorePoolSize(1);
            executor.initialize();
            executors.add(executor);
        }
        log.info("Initialized shared pool , shard num {}", threadNum);
    }

    public void execute(ShardTask task) {
        int shardId = task.getShardMethod().shard(task.getShardKey(), executors.size());
        if (shardId >= executors.size()) {
            throw new IndexOutOfBoundsException();
        }
        executors.get(shardId).execute(task);
    }

    public interface ShardMethod {
        int shard(String key, int bucketNum);
    }

    public static abstract class ShardTask implements Runnable {
        @Getter
        private final String shardKey;
        @Getter
        private final ShardMethod shardMethod;

        public ShardTask(String shardKey, ShardMethod shardMethod) {
            this.shardKey = shardKey;
            this.shardMethod = shardMethod;
        }
    }
}
