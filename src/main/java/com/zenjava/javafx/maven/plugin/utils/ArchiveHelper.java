package com.zenjava.javafx.maven.plugin.utils;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 *
 * @author FibreFoX
 */
public class ArchiveHelper {

    public void pack(final Path folder, final Path zipFilePath) throws IOException {
        // source of inspiration: http://stackoverflow.com/a/35158142/1961102
        try(FileOutputStream fos = new FileOutputStream(zipFilePath.toFile()); ZipOutputStream zos = new ZipOutputStream(fos)){
            Files.walkFileTree(folder, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    // when being on windows, it seems to break ZIP on other systems, so replace backslashes
                    zos.putNextEntry(new ZipEntry(folder.relativize(file).toString().replace("\\", "/")));
                    Files.copy(file, zos);
                    zos.closeEntry();
                    return FileVisitResult.CONTINUE;
                }
            });
        }
    }

}
