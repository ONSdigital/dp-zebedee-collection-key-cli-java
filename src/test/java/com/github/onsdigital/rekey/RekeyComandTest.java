package com.github.onsdigital.rekey;

import com.github.onsdigital.rekey.config.Config;
import com.github.onsdigital.rekey.config.ConfigParser;
import com.github.onsdigital.rekey.encryption.CollectionKey;
import com.github.onsdigital.rekey.encryption.KeyDecryptor;
import com.github.onsdigital.rekey.encryption.KeyEncryptor;
import com.github.onsdigital.rekey.files.FileArchiver;
import com.github.onsdigital.rekey.files.FilesHelper;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertThrows;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

public class RekeyComandTest {

    @Mock
    private ConfigParser parser;

    @Mock
    private KeyDecryptor decryptor;

    @Mock
    private KeyEncryptor encryptor;

    @Mock
    private FilesHelper filesHelper;

    @Mock
    private FileArchiver archiver;

    @Mock
    private SecretKey key, newKey, collectionKey;

    @Mock
    private IvParameterSpec iv, newIv;

    private RekeyComand cmd;
    private Config cfg;
    private Predicate<Path> keyFileFilter;
    private Path keyringDir, keyringBackupDir, keyringBackupTar, zebedeeDir;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        this.cmd = new RekeyComand(parser, decryptor, encryptor, archiver, filesHelper, keyFileFilter);

        FieldUtils.writeField(cmd, "key", "key", true);
        FieldUtils.writeField(cmd, "iv", "iv", true);
        FieldUtils.writeField(cmd, "newKey", "newKey", true);
        FieldUtils.writeField(cmd, "newIv", "newIv", true);
        FieldUtils.writeField(cmd, "zebedeeDir", "zebedeeDir", true);

        zebedeeDir = Paths.get("zebedee_root");
        keyringDir = zebedeeDir.resolve("keyring");
        keyringBackupDir = zebedeeDir.resolve("keyring-backup");

        cfg = new Config(key, iv, newKey, newIv, zebedeeDir, keyringDir, keyringBackupDir);
    }

    @Test
    public void parserConfigError_shouldThrowEx() throws Exception {
        doThrow(RekeyException.class)
                .when(parser)
                .parseConfig("key", "iv", "newKey", "newIv", "zebedeeDir");

        assertThrows(RekeyException.class, () -> cmd.call());

        verify(parser, times(1)).parseConfig("key", "iv", "newKey", "newIv", "zebedeeDir");
        verifyZeroInteractions(archiver, filesHelper, encryptor, decryptor);
    }

    @Test
    public void createTarGzError_shouldThrowEx() throws Exception {
        when(parser.parseConfig("key", "iv", "newKey", "newIv", "zebedeeDir"))
                .thenReturn(cfg);

        doThrow(RekeyException.class)
                .when(archiver)
                .createTarGz(cfg.getKeyringDir(), cfg.getKeyringBackupTar(), keyFileFilter);

        assertThrows(RekeyException.class, () -> cmd.call());

        verify(parser, times(1)).parseConfig("key", "iv", "newKey", "newIv", "zebedeeDir");
        verify(archiver, times(1)).createTarGz(cfg.getKeyringDir(), cfg.getKeyringBackupTar(), keyFileFilter);
        verifyZeroInteractions(filesHelper, encryptor, decryptor);
    }

    @Test
    public void moveDirError_shouldThrowEx() throws Exception {
        when(parser.parseConfig("key", "iv", "newKey", "newIv", "zebedeeDir"))
                .thenReturn(cfg);

        doThrow(RekeyException.class)
                .when(filesHelper)
                .move(cfg.getKeyringDir(), cfg.getKeyringBackupDir());

        assertThrows(RekeyException.class, () -> cmd.call());

        verify(parser, times(1)).parseConfig("key", "iv", "newKey", "newIv", "zebedeeDir");
        verify(archiver, times(1)).createTarGz(cfg.getKeyringDir(), cfg.getKeyringBackupTar(), keyFileFilter);
        verify(filesHelper, times(1)).move(cfg.getKeyringDir(), cfg.getKeyringBackupDir());

        verifyZeroInteractions(encryptor, decryptor);
    }

    @Test
    public void createDirError_shouldThrowEx() throws Exception {
        when(parser.parseConfig("key", "iv", "newKey", "newIv", "zebedeeDir"))
                .thenReturn(cfg);

        doThrow(RekeyException.class)
                .when(filesHelper)
                .createDir(cfg.getKeyringDir());

        assertThrows(RekeyException.class, () -> cmd.call());

        verify(parser, times(1)).parseConfig("key", "iv", "newKey", "newIv", "zebedeeDir");
        verify(archiver, times(1)).createTarGz(cfg.getKeyringDir(), cfg.getKeyringBackupTar(), keyFileFilter);
        verify(filesHelper, times(1)).move(cfg.getKeyringDir(), cfg.getKeyringBackupDir());
        verify(filesHelper, times(1)).createDir(cfg.getKeyringDir());

        verifyZeroInteractions(encryptor, decryptor);
    }

    @Test
    public void decryptorErr_shouldThrowEx() throws Exception {
        when(parser.parseConfig("key", "iv", "newKey", "newIv", "zebedeeDir"))
                .thenReturn(cfg);

        doThrow(RekeyException.class)
                .when(decryptor)
                .decreptKeys(cfg.getKeyringBackupDir(), cfg.getKey(), cfg.getIv());

        assertThrows(RekeyException.class, () -> cmd.call());

        verify(parser, times(1)).parseConfig("key", "iv", "newKey", "newIv", "zebedeeDir");
        verify(archiver, times(1)).createTarGz(cfg.getKeyringDir(), cfg.getKeyringBackupTar(), keyFileFilter);
        verify(filesHelper, times(1)).move(cfg.getKeyringDir(), cfg.getKeyringBackupDir());
        verify(filesHelper, times(1)).createDir(cfg.getKeyringDir());
        verify(decryptor, times(1)).decreptKeys(cfg.getKeyringBackupDir(), cfg.getKey(), cfg.getIv());

        verifyZeroInteractions(encryptor);
    }

    @Test
    public void encryptorErr_shouldThrowEx() throws Exception {
        when(parser.parseConfig("key", "iv", "newKey", "newIv", "zebedeeDir"))
                .thenReturn(cfg);

        List<CollectionKey> keys = new ArrayList<CollectionKey>() {{
            add(new CollectionKey(collectionKey, "abc123"));
        }};
        when(decryptor.decreptKeys(cfg.getKeyringBackupDir(), cfg.getKey(), cfg.getIv()))
                .thenReturn(keys);

        doThrow(RekeyException.class)
                .when(encryptor)
                .encryptToFile(keys, cfg.getKeyringDir(), cfg.getNewKey(), cfg.getNewIV());

        assertThrows(RekeyException.class, () -> cmd.call());

        verify(parser, times(1)).parseConfig("key", "iv", "newKey", "newIv", "zebedeeDir");
        verify(archiver, times(1)).createTarGz(cfg.getKeyringDir(), cfg.getKeyringBackupTar(), keyFileFilter);
        verify(filesHelper, times(1)).move(cfg.getKeyringDir(), cfg.getKeyringBackupDir());
        verify(filesHelper, times(1)).createDir(cfg.getKeyringDir());
        verify(decryptor, times(1)).decreptKeys(cfg.getKeyringBackupDir(), cfg.getKey(), cfg.getIv());
        verify(encryptor, times(1)).encryptToFile(keys, cfg.getKeyringDir(), cfg.getNewKey(), cfg.getNewIV());
    }

    @Test
    public void deleteDirErr_shouldThrowEx() throws Exception {
        when(parser.parseConfig("key", "iv", "newKey", "newIv", "zebedeeDir"))
                .thenReturn(cfg);

        List<CollectionKey> keys = new ArrayList<CollectionKey>() {{
            add(new CollectionKey(collectionKey, "abc123"));
        }};
        when(decryptor.decreptKeys(cfg.getKeyringBackupDir(), cfg.getKey(), cfg.getIv()))
                .thenReturn(keys);

        doThrow(RekeyException.class)
                .when(filesHelper)
                .deleteDir(cfg.getKeyringBackupDir());

        assertThrows(RekeyException.class, () -> cmd.call());

        verify(parser, times(1)).parseConfig("key", "iv", "newKey", "newIv", "zebedeeDir");
        verify(archiver, times(1)).createTarGz(cfg.getKeyringDir(), cfg.getKeyringBackupTar(), keyFileFilter);
        verify(filesHelper, times(1)).move(cfg.getKeyringDir(), cfg.getKeyringBackupDir());
        verify(filesHelper, times(1)).createDir(cfg.getKeyringDir());
        verify(decryptor, times(1)).decreptKeys(cfg.getKeyringBackupDir(), cfg.getKey(), cfg.getIv());
        verify(encryptor, times(1)).encryptToFile(keys, cfg.getKeyringDir(), cfg.getNewKey(), cfg.getNewIV());
        verify(filesHelper, times(1)).deleteDir(cfg.getKeyringBackupDir());
    }

    @Test
    public void testSuccess() throws Exception {
        when(parser.parseConfig("key", "iv", "newKey", "newIv", "zebedeeDir"))
                .thenReturn(cfg);

        List<CollectionKey> keys = new ArrayList<CollectionKey>() {{
            add(new CollectionKey(collectionKey, "abc123"));
        }};
        when(decryptor.decreptKeys(cfg.getKeyringBackupDir(), cfg.getKey(), cfg.getIv()))
                .thenReturn(keys);

        int exitCode = cmd.call();

        assertThat(exitCode, equalTo(0));
        verify(parser, times(1)).parseConfig("key", "iv", "newKey", "newIv", "zebedeeDir");
        verify(archiver, times(1)).createTarGz(cfg.getKeyringDir(), cfg.getKeyringBackupTar(), keyFileFilter);
        verify(filesHelper, times(1)).move(cfg.getKeyringDir(), cfg.getKeyringBackupDir());
        verify(filesHelper, times(1)).createDir(cfg.getKeyringDir());
        verify(decryptor, times(1)).decreptKeys(cfg.getKeyringBackupDir(), cfg.getKey(), cfg.getIv());
        verify(encryptor, times(1)).encryptToFile(keys, cfg.getKeyringDir(), cfg.getNewKey(), cfg.getNewIV());
        verify(filesHelper, times(1)).deleteDir(cfg.getKeyringBackupDir());
    }
}
