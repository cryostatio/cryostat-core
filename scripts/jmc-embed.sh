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
