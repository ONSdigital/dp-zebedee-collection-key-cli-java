package com.github.onsdigital.rekey.files;

import com.github.onsdigital.rekey.RekeyException;

import java.nio.file.Path;
import java.util.function.Predicate;

/**
 * Provides helper methods for creating File archives.
 */
public interface FileArchiver {

    /**
     * Create a tar.gz file for the specified src {@link Path}.
     *
     * @param src    the file/dir to archive.
     * @param dest   the destination of the tar to create.
     * @param filter a {@link Predicate} to filter which files should be added to the tar.
     * @throws RekeyException problem creating tar file.
     */
    void createTarGz(Path src, Path dest, Predicate<Path> filter) throws RekeyException;
}
