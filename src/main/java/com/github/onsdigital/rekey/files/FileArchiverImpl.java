package com.github.onsdigital.rekey.files;

import com.github.onsdigital.rekey.RekeyException;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorOutputStream;

import java.io.BufferedOutputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.function.Predicate;

public class FileArchiverImpl implements FileArchiver {

    private FilesHelper filesHelper;

    /**
     * Create a new instance of the FileArchiver.
     *
     * @param filesHelper
     */
    public FileArchiverImpl(final FilesHelper filesHelper) {
        this.filesHelper = filesHelper;
    }

    @Override
    public void createTarGz(Path src, Path dest, Predicate<Path> filter) throws RekeyException {
        List<Path> files = filesHelper.listFiles(src, filter);
        tarFiles(dest, files);
    }

    private void tarFiles(Path output, List<Path> files) throws RekeyException {
        try (
                OutputStream fOut = Files.newOutputStream(output);
                BufferedOutputStream buffOut = new BufferedOutputStream(fOut);
                GzipCompressorOutputStream gzOut = new GzipCompressorOutputStream(buffOut);
                TarArchiveOutputStream tarOut = new TarArchiveOutputStream(gzOut)
        ) {
            // Required to handle long file names.
            tarOut.setLongFileMode(TarArchiveOutputStream.LONGFILE_POSIX);

            for (Path p : files) {
                TarArchiveEntry tarEntry = new TarArchiveEntry(p.toFile(), p.getFileName().toString());
                tarOut.putArchiveEntry(tarEntry);

                Files.copy(p, tarOut);
                tarOut.closeArchiveEntry();
            }

            tarOut.finish();
        } catch (Exception ex) {
            throw new RekeyException("error creating tar gz file", ex);
        }
    }
}
