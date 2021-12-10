package com.github.onsdigital.rekey.files;

import com.github.onsdigital.rekey.RekeyException;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorOutputStream;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.function.Predicate;

import static java.text.MessageFormat.format;
import static org.apache.commons.io.FilenameUtils.getExtension;

public class FileArchiverImpl implements FileArchiver {

    private static final Logger LOG = LogManager.getLogger(FileArchiverImpl.class);

    @Override
    public void createTarGz(Path src, Path dest, Predicate<Path> filter) throws RekeyException {
        tarFiles(src, dest);
    }

    private void tarFiles(Path src, Path tarFile) throws RekeyException {
        try (
                OutputStream fOut = Files.newOutputStream(tarFile);
                BufferedOutputStream buffOut = new BufferedOutputStream(fOut);
                GzipCompressorOutputStream gzOut = new GzipCompressorOutputStream(buffOut);
                TarArchiveOutputStream tarOut = new TarArchiveOutputStream(gzOut)
        ) {
            // Required to handle long file names.
            tarOut.setLongFileMode(TarArchiveOutputStream.LONGFILE_POSIX);

            String backupDir = getBackupDirName(tarFile);

            Files.walkFileTree(src, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(Path p, BasicFileAttributes attrs) throws IOException {
                    if (!isTarget(p, attrs)) {
                        return FileVisitResult.CONTINUE;
                    }

                    String filename = getTarEntryName(backupDir, p);
                    LOG.info("tar entry: {}", p.toFile().getName());

                    TarArchiveEntry tarEntry = new TarArchiveEntry(p.toFile(), filename);
                    tarOut.putArchiveEntry(tarEntry);

                    Files.copy(p, tarOut);
                    tarOut.closeArchiveEntry();

                    return FileVisitResult.CONTINUE;
                }
            });

            tarOut.finish();
        } catch (Exception ex) {
            throw new RekeyException("error creating tar gz file", ex);
        }
    }

    private boolean isTarget(Path p, BasicFileAttributes attrs) {
        return !attrs.isSymbolicLink() && "txt".equals(getExtension(p.toString()));
    }

    private String getBackupDirName(Path tarFile) {
        return tarFile.getFileName().toString().replace(".tar.gz", "");
    }

    private String getTarEntryName(String backupDir, Path keyFile) {
        return Paths.get(backupDir).resolve(keyFile.getFileName()).toString();
    }
}
