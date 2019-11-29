package com.redhat.rhjmc.containerjfr.core.sys;

import java.time.Instant;
import java.util.concurrent.TimeUnit;

public class Clock {

    /**
     * @deprecated
     * Use #now()
     */
    @Deprecated
    public long getWallTime() {
        return System.currentTimeMillis();
    }

    public long getMonotonicTime() {
        return System.nanoTime();
    }

    public Instant now() {
        return Instant.now();
    }

    public void sleep(int millis) throws InterruptedException {
        sleep(TimeUnit.MILLISECONDS, millis);
    }

    public void sleep(TimeUnit unit, int quant) throws InterruptedException {
        Thread.sleep(unit.toMillis(quant));
    }

}
