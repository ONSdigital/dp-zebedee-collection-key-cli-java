package com.github.onsdigital.rekey.files;

import com.github.onsdigital.rekey.RekeyException;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static junit.framework.TestCase.assertFalse;
import static junit.framework.TestCase.assertTrue;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertThrows;

public class FilesHelperImplTest {

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    private FilesHelper helper;

    @Before
    public void setup() throws Exception {
        this.helper = new FilesHelperImpl();
    }

    @After
    public void tearDown() {
        this.folder.delete();
    }

    @Test
    public void testMove() throws Exception {
        Path src = folder.newFolder("aaa").toPath();
        Path dest = src.getParent().resolve("bbb");

        helper.move(src, dest);

        assertTrue(Files.notExists(src));
        assertTrue(Files.exists(dest));
    }

    @Test
    public void testCreateDir() throws Exception {
        Path target = folder.getRoot().toPath().resolve("aaa");
        assertFalse(Files.exists(target));

        helper.createDir(target);
        assertTrue(Files.exists(target));
        assertTrue(Files.isDirectory(target));
    }

    @Test
    public void testDeleteDir() throws Exception {
        Path target = folder.newFolder("aaa").toPath();
        assertTrue(Files.exists(target));

        helper.deleteDir(target);

        assertTrue(Files.notExists(target));
    }

    @Test
    public void testListFiles_filterNull_shouldThrowEx() throws Exception {
        RekeyException ex = assertThrows(RekeyException.class,
                () -> helper.listFiles(folder.getRoot().toPath(), null));
    }

    @Test
    public void testListFilesSuccess() throws Exception {
        Path keyringDir = folder.newFolder("keyring").toPath();
        assertTrue(Files.exists(keyringDir));
        assertTrue(Files.isDirectory(keyringDir));

        Path file = folder.newFile("keyring/test123.json").toPath();
        assertTrue(Files.exists(file));
        assertTrue(Files.isRegularFile(file));

        List<Path> results = helper.listFiles(keyringDir, (f) -> Files.isRegularFile(f));

        assertThat(results.size(), equalTo(1));
        assertThat(results.get(0), equalTo(file));
    }

}
