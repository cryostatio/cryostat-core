package com.redhat.rhjmc.containerjfr.core.sys;

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

    public InputStream newInputStream(Path path, OpenOption... openOptions) throws IOException {
        return Files.newInputStream(path, openOptions);
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
}
