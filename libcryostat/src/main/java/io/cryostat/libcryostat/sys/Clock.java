/*
 * Copyright The Cryostat Authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.cryostat.libcryostat.sys;

import java.time.Instant;
import java.util.concurrent.TimeUnit;

public class Clock {

    /**
     * @deprecated Use #now()
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
