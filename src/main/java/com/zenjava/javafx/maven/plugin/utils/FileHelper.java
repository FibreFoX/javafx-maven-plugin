package com.zenjava.javafx.maven.plugin.utils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.apache.maven.plugin.logging.Log;

/**
 *
 * @author FibreFoX
 */
public class FileHelper {

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

    public void copyRecursive(Path sourceFolder, Path targetFolder, Log logger) throws IOException {
        Files.walkFileTree(sourceFolder, new FileVisitor<Path>() {

            @Override
            public FileVisitResult preVisitDirectory(Path subfolder, BasicFileAttributes attrs) throws IOException {
                // do create subfolder (if needed)
                Files.createDirectories(targetFolder.resolve(sourceFolder.relativize(subfolder)));
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFile(Path sourceFile, BasicFileAttributes attrs) throws IOException {
                // do copy, and replace, as the resource might already be existing
                Files.copy(sourceFile, targetFolder.resolve(sourceFolder.relativize(sourceFile)), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.COPY_ATTRIBUTES);
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFileFailed(Path source, IOException ioe) throws IOException {
                // don't fail, just inform user
                logger.warn(String.format("Couldn't copy resource %s with reason %s", source.toString(), ioe.getLocalizedMessage()));
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult postVisitDirectory(Path source, IOException ioe) throws IOException {
                // nothing to do here
                return FileVisitResult.CONTINUE;
            }
        });
    }

    public Map<String, Long> getFileSizes(File nativeOutputDir, List<String> files) {
        final Map<String, Long> fileSizes = new HashMap<>();
        files.stream().forEach(relativeFilePath -> {
            File file = new File(nativeOutputDir, relativeFilePath);
            // add the size for each file
            fileSizes.put(relativeFilePath, file.length());
        });
        return fileSizes;
    }
}
