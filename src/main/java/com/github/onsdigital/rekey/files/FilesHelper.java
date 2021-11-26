package com.github.onsdigital.rekey.files;

import com.github.onsdigital.rekey.RekeyException;

import java.nio.file.Path;
import java.util.List;
import java.util.function.Predicate;

/**
 * Class provides common helper method for dealing with {@link java.io.File}.
 */
public interface FilesHelper {

    /**
     * Move the src file/dir to the specified destination.
     *
     * @param src  the file/dir to move.
     * @param dest the destination to move the files to.
     * @throws RekeyException error moving the files.
     */
    void move(Path src, Path dest) throws RekeyException;

    /**
     * Create a new directory at the specified location.
     *
     * @param target the location to create the dir under.
     * @return the {@link Path} of the created dir.
     * @throws RekeyException error creating dir.
     */
    Path createDir(Path target) throws RekeyException;

    /**
     * Delete the specified dir.
     *
     * @param target the dir to delete.
     * @throws RekeyException error deleting dir.
     */
    void deleteDir(Path target) throws RekeyException;

    /**
     * List the files in specified dir that match the filter criteria.
     *
     * @param src    the root dir to list.
     * @param filter a {@link Predicate} to filter out which files to return.
     * @return files matching the filter criteria.
     * @throws RekeyException error listing the files.
     */
    List<Path> listFiles(Path src, Predicate<Path> filter) throws RekeyException;
}
