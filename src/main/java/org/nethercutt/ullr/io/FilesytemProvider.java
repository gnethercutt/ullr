package org.nethercutt.ullr.io;

import java.io.IOException;
import java.net.URI;
import java.nio.file.CopyOption;
import java.nio.file.FileSystem;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;

import com.upplication.s3fs.S3FileSystemProvider;

public class FilesytemProvider {

    public static FileSystem s3fs = new S3FileSystemProvider() {
        @Override
        public void move(Path source, Path target, CopyOption... options) throws IOException {
            // Eat the Atomic copy option. S3 PUTs are atomic, though renames cannot have this guarantee
            copy(source, target);
            delete(source);
        }
    }.newFileSystem(URI.create("s3:///"), Collections.emptyMap());

    public static Path toPath(String pathString) {
        return pathString.startsWith("s3:") ? s3fs.getPath(pathString.substring(4)) : Paths.get(pathString);
    }
}