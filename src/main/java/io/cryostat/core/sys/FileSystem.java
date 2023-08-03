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
package io.cryostat.core.sys;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.CopyOption;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.PosixFilePermission;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
        try (Stream<Path> stream = Files.list(path)) {
            return stream.map(p -> p.getFileName().toString()).collect(Collectors.toList());
        }
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

    public Path createFile(Path path, FileAttribute<?>... attrs) throws IOException {
        return Files.createFile(path, attrs);
    }

    public Path createTempFile(String prefix, String suffix, FileAttribute<?>... attrs)
            throws IOException {
        return Files.createTempFile(prefix, suffix, attrs);
    }

    public Path createTempFile(Path path, String prefix, String suffix, FileAttribute<?>... attrs)
            throws IOException {
        return Files.createTempFile(path, prefix, suffix, attrs);
    }

    public Path createDirectory(Path path, FileAttribute<?>... attrs) throws IOException {
        return Files.createDirectory(path, attrs);
    }

    public Path createDirectories(Path path, FileAttribute<?>... attrs) throws IOException {
        return Files.createDirectories(path, attrs);
    }

    public Path createTempDirectory(String prefix, FileAttribute<?>... attrs) throws IOException {
        return Files.createTempDirectory(prefix, attrs);
    }

    public Path createTempDirectory(Path path, String prefix, FileAttribute<?>... attrs)
            throws IOException {
        return Files.createTempDirectory(path, prefix, attrs);
    }

    public Path setPosixFilePermissions(Path path, Set<PosixFilePermission> perms)
            throws IOException {
        return Files.setPosixFilePermissions(path, perms);
    }
}
