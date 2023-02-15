package org.tron.defi.contract_mirror.utils;


import lombok.Getter;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.tron.defi.contract_mirror.config.ServerConfig;

import java.util.ArrayList;

public class ShardedThreadPool {
    private final ArrayList<ThreadPoolTaskExecutor> executors;

    public ShardedThreadPool(ServerConfig.ThreadPoolConfig threadPoolConfig) {
        executors = new ArrayList<>(threadPoolConfig.getThreadNum());
        for (int i = 0; i < threadPoolConfig.getThreadNum(); ++i) {
            ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
            executor.setCorePoolSize(1);
            executor.initialize();
            executors.add(executor);
        }
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
