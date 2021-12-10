package com.github.onsdigital.rekey.files;

import com.github.onsdigital.rekey.RekeyException;
import org.apache.commons.io.FileUtils;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class FilesHelperImpl implements FilesHelper {

    @Override
    public void move(Path src, Path dest) throws RekeyException {
        try {
            Files.move(src, dest);
        } catch (Exception ex) {
            throw new RekeyException("error moving keyring dir", ex);
        }
    }

    @Override
    public Path createDir(Path target) throws RekeyException {
        try {
            return Files.createDirectory(target);
        } catch (Exception ex) {
            throw new RekeyException("error creating new keyring dir", ex);
        }
    }

    @Override
    public void deleteDir(Path target) throws RekeyException {
        try {
            FileUtils.deleteDirectory(target.toFile());
        } catch (Exception ex) {
            throw new RekeyException("error removing keyring backup dir", ex);
        }
    }

    @Override
    public List<Path> listFiles(Path src, Predicate<Path> filter) throws RekeyException {
        if (filter == null) {
            throw new RekeyException("file filter required but was null");
        }

        try {
            return Files.list(src).filter(f -> filter.test(f)).collect(Collectors.toList());
        } catch (Exception ex) {
            throw new RekeyException("error listeing files", ex);
        }
    }

    @Override
    public boolean exists(Path p) {
        return Files.exists(p);
    }
}
