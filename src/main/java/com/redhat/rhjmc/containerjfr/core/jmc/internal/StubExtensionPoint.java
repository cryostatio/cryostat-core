/*-
 * #%L
 * Container JFR Core
 * %%
 * Copyright (C) 2020 Red Hat, Inc.
 * %%
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
 * #L%
 */
package com.redhat.rhjmc.containerjfr.core.jmc.internal;

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IContributor;
import org.eclipse.core.runtime.IExtension;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.InvalidRegistryObjectException;

public class StubExtensionPoint implements IExtensionPoint {
    private static int ID = 0;

    @Override
    public IConfigurationElement[] getConfigurationElements()
            throws InvalidRegistryObjectException {
        return new IConfigurationElement[0];
    }

    @Override
    public String getNamespace() throws InvalidRegistryObjectException {
        return "stub";
    }

    @Override
    public String getNamespaceIdentifier() throws InvalidRegistryObjectException {
        return "stubId";
    }

    @Override
    public IContributor getContributor() throws InvalidRegistryObjectException {
        return new IContributor() {
            @Override
            public String getName() {
                return "none";
            }
        };
    }

    @Override
    public IExtension getExtension(String extensionId) throws InvalidRegistryObjectException {
        return null;
    }

    @Override
    public IExtension[] getExtensions() throws InvalidRegistryObjectException {
        return new IExtension[0];
    }

    @Override
    public String getLabel() throws InvalidRegistryObjectException {
        return "label";
    }

    @Override
    public String getLabel(String locale) throws InvalidRegistryObjectException {
        return "label";
    }

    @Override
    public String getSchemaReference() throws InvalidRegistryObjectException {
        return null;
    }

    @Override
    public String getSimpleIdentifier() throws InvalidRegistryObjectException {
        return "StubExtensionPoint";
    }

    @Override
    public synchronized String getUniqueIdentifier() throws InvalidRegistryObjectException {
        return "stubId" + StubExtensionPoint.ID++;
    }

    @Override
    public boolean equals(Object o) {
        return this == o;
    }

    @Override
    public boolean isValid() {
        return true;
    }
}
