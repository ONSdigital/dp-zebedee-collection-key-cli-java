package com.github.onsdigital.rekey.files;

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.io.FileInputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static junit.framework.TestCase.assertTrue;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;

public class FileArchiverImplTest {

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    private FileArchiver archiver;
    private Path keyringDir, keyringBackupDir;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);

        this.keyringDir = folder.newFolder("keyring").toPath();
        this.keyringBackupDir = folder.newFolder("keyring-backup").toPath();
        this.archiver = new FileArchiverImpl();
    }

    @After
    public void tearDown() throws Exception {
        folder.delete();
    }

    @Test
    public void testCreateTarGz() throws Exception {
        Path output = keyringDir.resolve("keyring-backup.tar.gz");

        Path key1 = folder.newFile("keyring-backup/collection1.txt").toPath();
        Path key2 = folder.newFile("keyring-backup/collection2.txt").toPath();

        archiver.createTarGz(keyringBackupDir, output, (p) -> Files.isRegularFile(p));

        assertTrue(Files.exists(output));

        List<String> entries = getTarEntries(output);
        assertThat(entries.size(), equalTo(2));

        // The output tar.gz should contain the following structure:
        // keyring-backup
        //    /collection1.txt
        //    /collection2.txt
        String key1ExpectedEntry = folder.getRoot().toPath().relativize(key1).toString();
        assertTrue(entries.contains(key1ExpectedEntry));

        String key2ExpectedEntry = folder.getRoot().toPath().relativize(key2).toString();
        assertTrue(entries.contains(key2ExpectedEntry));
    }

    private List<String> getTarEntries(Path tarFile) throws Exception {
        List<String> entries = new ArrayList<>();
        try (
                FileInputStream fis = new FileInputStream(tarFile.toFile());
                GzipCompressorInputStream gis = new GzipCompressorInputStream(fis);
                TarArchiveInputStream tIn = new TarArchiveInputStream(gis)
        ) {
            TarArchiveEntry entry = tIn.getNextTarEntry();

            while (entry != null) {
                assertTrue(entry.isFile());
                entries.add(entry.getName());

                entry = tIn.getNextTarEntry();
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            throw ex;
        }

        return entries;
    }
}
