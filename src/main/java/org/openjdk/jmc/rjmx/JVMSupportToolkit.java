/*
 * Copyright (c) 2018, Oracle and/or its affiliates. All rights reserved.
 * 
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * The contents of this file are subject to the terms of either the Universal Permissive License
 * v 1.0 as shown at http://oss.oracle.com/licenses/upl
 *
 * or the following license:
 *
 * Redistribution and use in source and binary forms, with or without modification, are permitted
 * provided that the following conditions are met:
 * 
 * 1. Redistributions of source code must retain the above copyright notice, this list of conditions
 * and the following disclaimer.
 * 
 * 2. Redistributions in binary form must reproduce the above copyright notice, this list of
 * conditions and the following disclaimer in the documentation and/or other materials provided with
 * the distribution.
 * 
 * 3. Neither the name of the copyright holder nor the names of its contributors may be used to
 * endorse or promote products derived from this software without specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR
 * IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND
 * FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY
 * WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package org.openjdk.jmc.rjmx;

import javax.management.MBeanServerConnection;

import org.openjdk.jmc.common.version.JavaVersion;
import org.openjdk.jmc.common.version.JavaVersionSupport;
import org.openjdk.jmc.rjmx.internal.ServerToolkit;
import org.openjdk.jmc.rjmx.services.internal.HotspotManagementToolkit;
import org.openjdk.jmc.ui.common.jvm.JVMDescriptor;
import org.openjdk.jmc.ui.common.jvm.JVMType;

/**
 * Checks the JVM capabilities of a connection.
 */
public final class JVMSupportToolkit {

	private JVMSupportToolkit() {
		throw new IllegalArgumentException("Don't instantiate this toolkit"); //$NON-NLS-1$
	}

	/**
	 * Checks if the JVM is supported in respect to console features, and if not, returns a string
	 * array with title and message
	 *
	 * @param connection
	 *            the connection to check
	 * @return a string array with title and message if the jvm is not supported, otherwise
	 *         {@code null}.
	 */
	public static String[] checkConsoleSupport(IConnectionHandle connection) {
		String title = null;
		String message = null;
		if (ConnectionToolkit.isJRockit(connection)) {
			title = "JRockit Not Supported";
			message = "You are attempting to connect to a JRockit JVM. To connect to a JRockit JVM, please use JRockit Mission Control 4.1 for JRockit R28, and JRockit Mission Control 3.1 for JRockit R27. \nIf you connect to a JRockit, functionality will be limited. \n\nAre you sure you want to continue?";
		} else if (!ConnectionToolkit.isHotSpot(connection)) {
			title = "Not a Known JVM";
			message = "You are attempting to connect to an unknown JVM. This will most likely not succeed unless you're running the JMX Console, and even then functionality will be limited. \n\nAre you sure you want to continue?";
		} else if (!ConnectionToolkit.isJavaVersionAboveOrEqual(connection,
				JavaVersionSupport.DIAGNOSTIC_COMMANDS_SUPPORTED)) {
			title = "Too Old JVM";
			message = "The JVM you are connecting to is not recent enough for full functionality in the JMX Console. For full functionality you need to connect to an Oracle JDK 7u40 or later JVM or an OpenJDK 11+ JVM. Do you still want to connect?";
		}

		if (title != null) {
			String[] returnInfo = new String[2];
			returnInfo[0] = title;
			returnInfo[1] = message;
			return returnInfo;
		}
		return new String[0];
	}

	/**
	 * Checks if Flight Recorder is disabled.
	 *
	 * @param connection
	 *            the connection to check
	 * @param explicitFlag
	 *            If the flag has to be explicitly disabled on the command line with
	 *            -XX:-FlightRecorder
	 * @return If explicitFlag is true, then returns true only if Flight Recorder is explicitly
	 *         disabled on the command line. If explicitFlag is false, then returns true if Flight
	 *         Recorder is currently not enabled.
	 */
	public static boolean isFlightRecorderDisabled(IConnectionHandle connection, boolean explicitFlag) {
		try {
			MBeanServerConnection server = connection.getServiceOrThrow(MBeanServerConnection.class);
			boolean disabled = !Boolean
					.parseBoolean(HotspotManagementToolkit.getVMOption(server, "FlightRecorder").toString()); //$NON-NLS-1$
			if (explicitFlag) {
				return (disabled && HotspotManagementToolkit.isVMOptionExplicit(server, "FlightRecorder")); //$NON-NLS-1$
			} else {
				return disabled;
			}
		} catch (Exception e) {
			return false;
		}
	}

	/**
	 * Returns a descriptive error message about why Flight Recorder is unavailable.
	 *
	 * @param handle
	 *            the connection to check
	 * @param shortMessage
	 *            whether to return a short message or not
	 * @return a descriptive string about why Flight Recorder is unavailable.
	 */
	public static String getNoFlightRecorderErrorMessage(IConnectionHandle handle, boolean shortMessage) {
		if (ConnectionToolkit.isJRockit(handle)) {
			return getJfrJRockitNotSupported(shortMessage);
		}
		if (!ConnectionToolkit.isHotSpot(handle)) {
			return getJfrNonHotSpotNotSupported(shortMessage);
		}
		if (!ConnectionToolkit.isJavaVersionAboveOrEqual(handle, JavaVersionSupport.JFR_ENGINE_SUPPORTED)) {
			return getJfrOldHotSpotNotSupported(shortMessage);
		}
		if (isFlightRecorderDisabled(handle, true)) {
			return getJfrDisabled(shortMessage);
		}
		return getJfrNotEnabled(shortMessage);
	}

	/**
	 * Returns information about whether to server denoted by the handle supports Flight Recorder
	 *
	 * @param handle
	 *            the server to check
	 * @param shortMessage
	 *            whether to return a short message or not
	 * @return a descriptive string about why Flight Recorder is not supported, or {@code null}.
	 */
	public static String checkFlightRecorderSupport(IServerHandle handle, boolean shortMessage) {
		if (ServerToolkit.getJvmInfo(handle) != null) {
			JVMDescriptor jvmInfo = ServerToolkit.getJvmInfo(handle);

			if (jvmInfo.getJvmType() == null) {
				return null;
			}
			if (jvmInfo.getJvmType() == JVMType.JROCKIT) {
				return getJfrJRockitNotSupported(shortMessage);
			}
			if (jvmInfo.getJvmType() == JVMType.UNKNOWN) {
				return null;
			}
			if (jvmInfo.getJvmType() != JVMType.HOTSPOT) {
				return getJfrNonHotSpotNotSupported(shortMessage);
			}
			if (jvmInfo.getJavaVersion() == null) {
				return null;
			}
			if (!new JavaVersion(jvmInfo.getJavaVersion())
					.isGreaterOrEqualThan(JavaVersionSupport.JFR_ENGINE_SUPPORTED)) {
				return getJfrOldHotSpotNotSupported(shortMessage);
			}
			if (!new JavaVersion(jvmInfo.getJavaVersion())
					.isGreaterOrEqualThan(JavaVersionSupport.JFR_FULLY_SUPPORTED)) {
				return getJfrOldHotSpotNotFullySupported(shortMessage);
			}
		}
		return null;
	}

	private static String getJfrNotEnabled(boolean shortMessage) {
		return shortMessage ? "Flight Recorder features are not enabled"
				: "Flight Recorder features are not enabled. To enable this you need to use an Oracle JDK 7u4 or later JVM started with -XX:+UnlockCommercialFeatures -XX:+FlightRecorder or an OpenJDK 11+ JVM.";
	}

	private static String getJfrDisabled(boolean shortMessage) {
		return shortMessage ? "Flight Recorder is explicitly disabled"
				: "Flight Recorder is explicitly disabled. To enable the Flight Recorder, remove -XX:-FlightRecorder from the command line.";
	}

	private static String getJfrOldHotSpotNotSupported(boolean shortMessage) {
		return shortMessage ? "Flight Recorder is not supported for JVMs with Java versions below 7u4"
				: "Flight Recorder is not supported for JVMs with Java versions below 7u4";
	}

	private static String getJfrOldHotSpotNotFullySupported(boolean shortMessage) {
		return shortMessage ? "Flight Recorder is not fully supported for JVMs with Java versions below 7u40"
				: "Flight Recorder is not fully supported for JVMs with Java versions below 7u40";
	}

	private static String getJfrNonHotSpotNotSupported(boolean shortMessage) {
		return shortMessage ? "Flight Recorder is not supported for non HotSpot JVMs"
				: "Flight Recorder is not supported for non HotSpot JVMs";
	}

	private static String getJfrJRockitNotSupported(boolean shortMessage) {
		return shortMessage ? "JRockit is no longer supported"
				: "JRockit is no longer supported";
	}
}
