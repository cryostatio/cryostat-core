#!/bin/bash

#
# Copyright The Cryostat Authors
#
# The Universal Permissive License (UPL), Version 1.0
#
# Subject to the condition set forth below, permission is hereby granted to any
# person obtaining a copy of this software, associated documentation and/or data
# (collectively the "Software"), free of charge and under any and all copyright
# rights in the Software, and any and all patent rights owned or freely
# licensable by each licensor hereunder covering either (i) the unmodified
# Software as contributed to or provided by such licensor, or (ii) the Larger
# Works (as defined below), to deal in both
#
# (a) the Software, and
# (b) any piece of software and/or hardware listed in the lrgrwrks.txt file if
# one is included with the Software (each a "Larger Work" to which the Software
# is contributed by such licensors),
#
# without restriction, including without limitation the rights to copy, create
# derivative works of, display, perform, and distribute the Software and make,
# use, sell, offer for sale, import, export, have made, and have sold the
# Software and the Larger Work(s), and to sublicense the foregoing rights on
# either these or other terms.
#
# This license is subject to the following condition:
# The above copyright notice and either this complete permission notice or at
# a minimum a reference to the UPL must be included in all copies or
# substantial portions of the Software.
#
# THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
# IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
# FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
# AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
# LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
# OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
# SOFTWARE.
#

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
