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
package com.redhat.rhjmc.containerjfr.core.sys;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.CopyOption;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

public class FileSystem {

    public boolean isDirectory(Path path, LinkOption... linkOptions) {
        return Files.isDirectory(path, linkOptions);
    }

    public boolean isRegularFile(Path path, LinkOption... linkOptions) {
        return Files.isRegularFile(path, linkOptions);
    }

    public boolean isReadable(Path path) {
        return Files.isReadable(path);
    }

    public boolean isWritable(Path path) {
        return Files.isWritable(path);
    }

    public boolean isExecutable(Path path) {
        return Files.isExecutable(path);
    }

    public InputStream newInputStream(Path path, OpenOption... openOptions) throws IOException {
        return Files.newInputStream(path, openOptions);
    }

    public Path writeString(Path path, CharSequence content, OpenOption... openOptions)
            throws IOException {
        return Files.writeString(path, content, openOptions);
    }

    public BufferedReader readFile(Path path) throws IOException {
        return Files.newBufferedReader(path);
    }

    public String readString(Path path) throws IOException {
        return Files.readString(path);
    }

    public long copy(InputStream in, Path out, CopyOption... copyOptions) throws IOException {
        return Files.copy(in, out, copyOptions);
    }

    public List<String> listDirectoryChildren(Path path) throws IOException {
        return Files.list(path).map(p -> p.getFileName().toString()).collect(Collectors.toList());
    }

    public boolean deleteIfExists(Path path) throws IOException {
        return Files.deleteIfExists(path);
    }

    public boolean exists(Path path, LinkOption... linkOptions) {
        return Files.exists(path, linkOptions);
    }

    public Path pathOf(String first, String... more) {
        return Path.of(first, more);
    }

    public long size(Path path) throws IOException {
        return Files.size(path);
    }
}
