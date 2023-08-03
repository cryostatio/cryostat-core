#!/bin/bash

# Copyright The Cryostat Authors.
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
# http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

# This script copies JMC 7 classes used by Cryostat Core into
# the src/main/java directory
if [ $# -ne 1 ]; then
    echo "usage: $0 /path/to/jmc7" >&2
    exit 1
fi

JMC_DIR=$1
ROOT_DIR="$(readlink -f "$(dirname "$(readlink -f "${BASH_SOURCE[0]}")")/..")"

copyBundle () {
    local bundle_name="$1"
    cp -rv "${JMC_DIR}/application/${bundle_name}/src/main/java/org" "${ROOT_DIR}/src/main/java/"
}

copyResource() {
    local bundle_name="$1"
    local resource_path="$2"

    mkdir -p "$(dirname "${ROOT_DIR}/src/main/resources/${resource_path}")"
    cp -rv "${JMC_DIR}/application/${bundle_name}/src/main/resources/${resource_path}" "${ROOT_DIR}/src/main/resources/${resource_path}"
}
# src/main/resources/org/openjdk/jmc/rjmx/subscription/internal/mrimetadata.xml
copyBundle "org.openjdk.jmc.flightrecorder.configuration"
copyBundle "org.openjdk.jmc.flightrecorder.controlpanel.ui"
copyBundle "org.openjdk.jmc.flightrecorder.controlpanel.ui.configuration"
copyBundle "org.openjdk.jmc.rjmx"
copyBundle "org.openjdk.jmc.rjmx.services.jfr"
copyBundle "org.openjdk.jmc.jdp"
copyBundle "org.openjdk.jmc.ui.common"

copyResource "org.openjdk.jmc.flightrecorder.configuration" "org/openjdk/jmc/flightrecorder/configuration/events/jfc_v1.xsd"
copyResource "org.openjdk.jmc.flightrecorder.configuration" "org/openjdk/jmc/flightrecorder/configuration/events/jfc_v2.xsd"
copyResource "org.openjdk.jmc.rjmx" "org/openjdk/jmc/rjmx/subscription/internal/mrimetadata.xml"
