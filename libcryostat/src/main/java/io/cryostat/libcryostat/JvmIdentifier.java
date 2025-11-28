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
package io.cryostat.libcryostat;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Base64;
import java.util.Objects;

import javax.management.JMX;
import javax.management.MBeanServerConnection;
import javax.management.ObjectName;

import io.cryostat.libcryostat.net.IDException;
import io.cryostat.libcryostat.net.RuntimeMetrics;

import org.apache.commons.codec.digest.DigestUtils;

public class JvmIdentifier {

    private final String hash;

    private JvmIdentifier(String hash) {
        this.hash = hash;
    }

    public String getHash() {
        return hash;
    }

    public static JvmIdentifier getLocal() throws IDException {
        return getLocal(getAgentId());
    }

    public static JvmIdentifier getLocal(String id) throws IDException {
        return from(id, RuntimeMetrics.readLocalMetrics());
    }

    public static JvmIdentifier from(String id, RuntimeMetrics metrics) throws IDException {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream(512);
                DataOutputStream dos = new DataOutputStream(baos)) {
            safeWrite(dos, id);
            safeWrite(dos, metrics.getClassPath());
            safeWrite(dos, metrics.getName());
            safeWrite(dos, stringifyArray(metrics.getInputArguments()));
            safeWrite(dos, metrics.getLibraryPath());
            safeWrite(dos, metrics.getVmVendor());
            safeWrite(dos, metrics.getVmVersion());
            dos.writeLong(metrics.getStartTime());
            byte[] hash = DigestUtils.sha256(baos.toByteArray());
            return new JvmIdentifier(
                    new String(Base64.getUrlEncoder().encode(hash), StandardCharsets.UTF_8).trim());
        } catch (IOException e) {
            throw new IDException(e);
        }
    }

    private static void safeWrite(DataOutputStream dos, String value) throws IOException {
        if (value == null) {
            return;
        }
        dos.writeUTF(value);
    }

    private static String getAgentId() {
        try {
            return getAgentId(ManagementFactory.getPlatformMBeanServer());
        } catch (SecurityException se) {
            return null;
        }
    }

    public static String getAgentId(MBeanServerConnection mbs) {
        try {
            CryostatAgentMXBean agentMXBean =
                    JMX.newMXBeanProxy(
                            mbs,
                            new ObjectName("io.cryostat.agent.CryostatAgent:name=agent"),
                            CryostatAgentMXBean.class);
            return agentMXBean.getId();
        } catch (Exception e) {
            return null;
        }
    }

    private static String stringifyArray(Object arrayObject) {
        if (arrayObject == null) {
            return null;
        }
        String stringified;
        String componentType = arrayObject.getClass().getComponentType().toString();
        switch (componentType) {
            case "boolean":
                stringified = Arrays.toString((boolean[]) arrayObject);
                break;

            case "byte":
                stringified = Arrays.toString((byte[]) arrayObject);
                break;

            case "char":
                stringified = Arrays.toString((char[]) arrayObject);
                break;

            case "short":
                stringified = Arrays.toString((short[]) arrayObject);
                break;

            case "int":
                stringified = Arrays.toString((int[]) arrayObject);
                break;

            case "long":
                stringified = Arrays.toString((long[]) arrayObject);
                break;

            case "float":
                stringified = Arrays.toString((float[]) arrayObject);
                break;

            case "double":
                stringified = Arrays.toString((double[]) arrayObject);
                break;

            default:
                stringified = Arrays.toString((Object[]) arrayObject);
        }
        return stringified;
    }

    @Override
    public String toString() {
        return hash;
    }

    @Override
    public int hashCode() {
        return Objects.hash(hash);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        JvmIdentifier other = (JvmIdentifier) obj;
        return Objects.equals(hash, other.hash);
    }

    static interface CryostatAgentMXBean {
        public String getId();
    }
}
