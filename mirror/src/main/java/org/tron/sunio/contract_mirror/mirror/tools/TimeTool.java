package org.tron.sunio.contract_mirror.mirror.tools;

import java.time.Instant;

public class TimeTool {
    // 睡眠 millis 毫秒
    public static void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (Exception ignored) {
        }
    }

    public static long epochSeconds() {
        return System.currentTimeMillis() / 1_000L;
    }

    public static Long setSeconds(Long milliseconds) {
        return Instant.ofEpochMilli(milliseconds).getEpochSecond();
    }
}
