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
package io.cryostat.core;

import java.io.IOException;
import java.io.InputStream;
import java.util.logging.LogManager;

import io.cryostat.core.jmc.SecurityManager;
import io.cryostat.core.reports.InterruptibleReportGenerator.ReportRuleEvalEvent;

import jdk.jfr.FlightRecorder;

public class CryostatCore {
    private CryostatCore() {}

    public static void initialize() throws IOException {
        System.setProperty(
                "org.openjdk.jmc.common.security.manager",
                SecurityManager.class.getCanonicalName());

        try (InputStream config =
                CryostatCore.class.getResourceAsStream("/config/logging.properties")) {
            LogManager.getLogManager()
                    .updateConfiguration(config, k -> ((o, n) -> o != null ? o : n));
        }

        FlightRecorder.register(ReportRuleEvalEvent.class);
    }
}
