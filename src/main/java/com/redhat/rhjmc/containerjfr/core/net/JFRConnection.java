package com.redhat.rhjmc.containerjfr.core.net;

import java.io.IOException;

import org.openjdk.jmc.rjmx.ConnectionToolkit;
import org.openjdk.jmc.rjmx.IConnectionDescriptor;
import org.openjdk.jmc.rjmx.IConnectionHandle;
import org.openjdk.jmc.rjmx.IConnectionListener;
import org.openjdk.jmc.rjmx.internal.DefaultConnectionHandle;
import org.openjdk.jmc.rjmx.internal.RJMXConnection;
import org.openjdk.jmc.rjmx.internal.ServerDescriptor;
import org.openjdk.jmc.rjmx.services.jfr.IFlightRecorderService;
import org.openjdk.jmc.rjmx.services.jfr.internal.FlightRecorderServiceFactory;
import org.openjdk.jmc.rjmx.services.jfr.internal.FlightRecorderServiceV2;

import com.redhat.rhjmc.containerjfr.core.sys.Clock;
import com.redhat.rhjmc.containerjfr.core.tui.ClientWriter;

public class JFRConnection implements AutoCloseable {

    public static final int DEFAULT_PORT = 9091;

    private final ClientWriter cw;
    private final RJMXConnection rjmxConnection;
    private final IConnectionHandle handle;
    private final IFlightRecorderService service;

    JFRConnection(ClientWriter cw, IConnectionDescriptor cd) throws Exception {
        this.cw = cw;
        this.rjmxConnection = attemptConnect(cd);
        this.handle =
                new DefaultConnectionHandle(
                        rjmxConnection, "RJMX Connection", new IConnectionListener[0]);
        this.service = new FlightRecorderServiceFactory().getServiceInstance(handle);
    }

    public IConnectionHandle getHandle() {
        return this.handle;
    }

    public IFlightRecorderService getService() {
        return this.service;
    }

    public long getApproximateServerTime(Clock clock) {
        return this.rjmxConnection.getApproximateServerTime(clock.getWallTime());
    }

    public String getHost() {
        try {
            return ConnectionToolkit.getHostName(
                    this.rjmxConnection.getConnectionDescriptor().createJMXServiceURL());
        } catch (IOException e) {
            cw.println(e);
            return "unknown";
        }
    }

    public int getPort() {
        try {
            return ConnectionToolkit.getPort(
                    this.rjmxConnection.getConnectionDescriptor().createJMXServiceURL());
        } catch (IOException e) {
            cw.println(e);
            return 0;
        }
    }

    public boolean isV1() {
        return !isV2();
    }

    public boolean isV2() {
        return FlightRecorderServiceV2.isAvailable(this.handle);
    }

    public void disconnect() {
        this.rjmxConnection.close();
    }

    @Override
    public void close() {
        this.disconnect();
    }

    private RJMXConnection attemptConnect(IConnectionDescriptor cd) throws Exception {
        try {
            RJMXConnection conn =
                    new RJMXConnection(cd, new ServerDescriptor(), JFRConnection::failConnection);
            if (!conn.connect()) {
                failConnection();
            }
            return conn;
        } catch (Exception e) {
            cw.println("connection attempt failed.");
            throw e;
        }
    }

    private static void failConnection() {
        throw new RuntimeException("Connection Failed");
    }
}
