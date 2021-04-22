/*
 * Copyright the Cryostat Authors
 *
 * The Universal Permissive License (UPL), Version 1.0
 *
 * Subject to the condition set forth below, permission is hereby granted to any
 * person obtaining a copy of this software, associated documentation and/or data
 * (collectively the "Software"), free of charge and under any and all copyright
 * rights in the Software, and any and all patent rights owned or freely
 * licensable by each licensor hereunder covering either (i) the unmodified
 * Software as contributed to or provided by such licensor, or (ii) the Larger
 * Works (as defined below), to deal in both
 *
 * (a) the Software, and
 * (b) any piece of software and/or hardware listed in the lrgrwrks.txt file if
 * one is included with the Software (each a "Larger Work" to which the Software
 * is contributed by such licensors),
 *
 * without restriction, including without limitation the rights to copy, create
 * derivative works of, display, perform, and distribute the Software and make,
 * use, sell, offer for sale, import, export, have made, and have sold the
 * Software and the Larger Work(s), and to sublicense the foregoing rights on
 * either these or other terms.
 *
 * This license is subject to the following condition:
 * The above copyright notice and either this complete permission notice or at
 * a minimum a reference to the UPL must be included in all copies or
 * substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package io.cryostat.core.jmc;

import java.util.Map;

import javax.management.ObjectName;

import org.openjdk.jmc.common.unit.IQuantity;
import org.openjdk.jmc.rjmx.services.jfr.IRecordingDescriptor;

public class CopyRecordingDescriptor implements IRecordingDescriptor {
    private final IRecordingDescriptor original;

    public CopyRecordingDescriptor(IRecordingDescriptor original) {
        this.original = original;
    }

    @Override
    public String getName() {
        return original.getName();
    }

    @Override
    public Long getId() {
        return original.getId();
    }

    @Override
    public RecordingState getState() {
        return original.getState();
    }

    @Override
    public Map<String, ?> getOptions() {
        return original.getOptions();
    }

    @Override
    public ObjectName getObjectName() {
        return original.getObjectName();
    }

    @Override
    public IQuantity getDataStartTime() {
        return original.getDataStartTime();
    }

    @Override
    public IQuantity getDataEndTime() {
        return original.getDataEndTime();
    }

    @Override
    public IQuantity getStartTime() {
        return original.getStartTime();
    }

    @Override
    public IQuantity getDuration() {
        return original.getDuration();
    }

    @Override
    public boolean isContinuous() {
        return original.isContinuous();
    }

    @Override
    public boolean getToDisk() {
        return original.getToDisk();
    }

    @Override
    public IQuantity getMaxSize() {
        return original.getMaxSize();
    }

    @Override
    public IQuantity getMaxAge() {
        return original.getMaxAge();
    }
}
