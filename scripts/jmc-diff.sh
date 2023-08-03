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

# This script compares the differences between a JMC 7 source and the
# embedded JMC source files within this repo
if [ $# -ne 1 ]; then
    echo "usage: $0 /path/to/jmc7" >&2
    exit 1
fi

JMC_DIR=$1
ROOT_DIR="$(readlink -f "$(dirname "$(readlink -f "${BASH_SOURCE[0]}")")/..")"
SUMMARIZE_MISSING="${SUMMARIZE_MISSING:-false}"
MISSING_FILES=""

diffBundle () {
    local bundle_name="$1"
    local src_dir="$2"
    local exclude_dir="$3"

    pushd "${JMC_DIR}/application/${bundle_name}" >/dev/null
    if [ -n "$exclude_dir" ]; then
        local srcFiles=$(find "${src_dir}" -type f -not -path "${exclude_dir}/*")
    else
        local srcFiles=$(find "${src_dir}" -type f)
    fi

    local diff_flags="-u"
    if [ "${SUMMARIZE_MISSING}" != true ]; then
        diff_flags="${diff_flags} -N"
    fi

    for i in ${srcFiles}; do
        if [ "${SUMMARIZE_MISSING}" = true -a ! -f "${ROOT_DIR}/$i" ]; then
            MISSING_FILES="${MISSING_FILES}\n$i"
        else
            diff ${diff_flags} --label="a/$i" --label="b/$i" "${JMC_DIR}/application/${bundle_name}/$i" "${ROOT_DIR}/$i"
        fi
    done
    popd >/dev/null
}

diffBundle "org.openjdk.jmc.flightrecorder.configuration" "src/main/java/org/openjdk/jmc/flightrecorder/configuration/"
diffBundle "org.openjdk.jmc.flightrecorder.controlpanel.ui" "src/main/java/org/openjdk/jmc/flightrecorder/controlpanel/ui" "src/main/java/org/openjdk/jmc/flightrecorder/controlpanel/ui/configuration"
diffBundle "org.openjdk.jmc.flightrecorder.controlpanel.ui.configuration" "src/main/java/org/openjdk/jmc/flightrecorder/controlpanel/ui/configuration"
diffBundle "org.openjdk.jmc.rjmx" "src/main/java/org/openjdk/jmc/rjmx" "src/main/java/org/openjdk/jmc/rjmx/services/jfr"
diffBundle "org.openjdk.jmc.rjmx.services.jfr" "src/main/java/org/openjdk/jmc/rjmx/services/jfr"
diffBundle "org.openjdk.jmc.jdp" "src/main/java/org/openjdk/jmc/jdp"
diffBundle "org.openjdk.jmc.ui.common" "src/main/java/org/openjdk/jmc/ui/common"

if [ "${SUMMARIZE_MISSING}" = true ]; then
    echo -e "\nRemoved files:${MISSING_FILES}"
fi
