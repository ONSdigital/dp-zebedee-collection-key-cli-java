package com.github.onsdigital.rekey.config;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.File;
import java.nio.file.Path;
import java.util.Base64;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertThrows;

public class ConfigParserImplTest {

    private static final String TEST_SECRET_KEY = "9ayoex3J26PPAFugNlWP7A==";
    private static final String TEST_INIT_VECTOR = "jODFzQ8ktuiUQRobxOzSJQ";

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    private Path zebedeeRoot;
    private Path keyringDir;
    private ConfigParser parser;

    @Before
    public void setUp() throws Exception {
        parser = new ConfigParserImpl();

        this.zebedeeRoot = folder.newFolder("zebedee").toPath();
        this.keyringDir = zebedeeRoot.resolve("keyring");
    }

    @After
    public void tearDown() throws Exception {
        folder.delete();
    }

    @Test
    public void parseConfig_KeyNull_shouldThrowException() {
        Exception ex = assertThrows(Exception.class, () -> parser.parseConfig(null, null, null, null, null));

        assertThat(ex.getMessage(), equalTo("secret key value required but was null/empty"));
    }

    @Test
    public void parseConfig_KeyEmpty_shouldThrowException() {
        Exception ex = assertThrows(Exception.class, () -> parser.parseConfig("", null, null, null, null));

        assertThat(ex.getMessage(), equalTo("secret key value required but was null/empty"));
    }

    @Test
    public void parseConfig_KeyNonBase64Value_shouldThrowException() {
        Exception ex = assertThrows(Exception.class,
                () -> parser.parseConfig("Not Base64 value", null, null, null, null));

        assertThat(ex.getMessage(), equalTo("error Base64 decoding secret key string value"));
    }

    @Test
    public void parseConfig_ivNull_shouldThrowException() {
        Exception ex = assertThrows(Exception.class,
                () -> parser.parseConfig(TEST_SECRET_KEY, null, null, null, null));

        assertThat(ex.getMessage(), equalTo("init vector value required but was null/empty"));
    }

    @Test
    public void parseConfig_ivEmptyshouldThrowException() {
        Exception ex = assertThrows(Exception.class,
                () -> parser.parseConfig(TEST_SECRET_KEY, "", null, null, null));

        assertThat(ex.getMessage(), equalTo("init vector value required but was null/empty"));
    }

    @Test
    public void parseConfig_IvNonBase64Value_shouldThrowException() {
        Exception ex = assertThrows(Exception.class,
                () -> parser.parseConfig(TEST_SECRET_KEY, "1 2 3", null, null, null));

        assertThat(ex.getMessage(), equalTo("error Base64 decoding init vector string value"));
    }

    @Test
    public void parseConfig_NewKeyNull_shouldThrowException() {
        Exception ex = assertThrows(Exception.class,
                () -> parser.parseConfig(TEST_SECRET_KEY, TEST_INIT_VECTOR, null, null, null));

        assertThat(ex.getMessage(), equalTo("secret key value required but was null/empty"));
    }

    @Test
    public void parseConfig_NewKeyEmpty_shouldThrowException() {
        Exception ex = assertThrows(Exception.class,
                () -> parser.parseConfig("", TEST_SECRET_KEY, TEST_INIT_VECTOR, null, null));

        assertThat(ex.getMessage(), equalTo("secret key value required but was null/empty"));
    }

    @Test
    public void parseConfig_NewKeyNonBase64Value_shouldThrowException() {
        Exception ex = assertThrows(Exception.class,
                () -> parser.parseConfig(TEST_SECRET_KEY, TEST_INIT_VECTOR, "One Two THree", null, null));

        assertThat(ex.getMessage(), equalTo("error Base64 decoding secret key string value"));
    }

    @Test
    public void parseConfig_ZebedeeRootDirNull_shouldThrowException() {
        Exception ex = assertThrows(Exception.class,
                () -> parser.parseConfig(TEST_SECRET_KEY, TEST_INIT_VECTOR, TEST_SECRET_KEY, TEST_INIT_VECTOR, null));

        assertThat(ex.getMessage(), equalTo("zebedee_root dir value required but was null/empty"));
    }

    @Test
    public void parseConfig_ZebedeeRootDirEmpty_shouldThrowException() {
        Exception ex = assertThrows(Exception.class,
                () -> parser.parseConfig(TEST_SECRET_KEY, TEST_INIT_VECTOR, TEST_SECRET_KEY, TEST_INIT_VECTOR, ""));

        assertThat(ex.getMessage(), equalTo("zebedee_root dir value required but was null/empty"));
    }

    @Test
    public void parseConfig_ZebedeeRootDirNotExist_shouldThrowException() {
        Exception ex = assertThrows(Exception.class,
                () -> parser.parseConfig(TEST_SECRET_KEY, TEST_INIT_VECTOR, TEST_SECRET_KEY, TEST_INIT_VECTOR, "ac/dc"));

        assertThat(ex.getMessage(), equalTo("ac/dc dir required but does not exist"));
    }

    @Test
    public void parseConfig_ZebedeeRootDirNotADir_shouldThrowException() throws Exception {
        Path dir = folder.newFile("data.json").toPath();

        Exception ex = assertThrows(Exception.class,
                () -> parser.parseConfig(TEST_SECRET_KEY, TEST_INIT_VECTOR, TEST_SECRET_KEY, TEST_INIT_VECTOR, dir.toString()));

        assertThat(ex.getMessage(), equalTo(dir.toString() + " is a file but expected directory"));
    }

    @Test
    public void parseConfig_KeyringDirNotFound_shouldThrowException() {
        Exception ex = assertThrows(Exception.class,
                () -> parser.parseConfig(
                        TEST_SECRET_KEY, TEST_INIT_VECTOR, TEST_SECRET_KEY, TEST_INIT_VECTOR, zebedeeRoot.toString()));

        assertThat(ex.getMessage(), equalTo(keyringDir.toString() + " dir required but does not exist"));
    }

    @Test
    public void parseConfig_KeyringDirNotADir_shouldThrowException() throws Exception {
        File dir = folder.newFile("zebedee/keying.txt");

        Exception ex = assertThrows(Exception.class,
                () -> parser.parseConfig(
                        TEST_SECRET_KEY, TEST_INIT_VECTOR, TEST_SECRET_KEY, TEST_INIT_VECTOR, dir.toString()));

        assertThat(ex.getMessage(), equalTo(dir.toString() + " is a file but expected directory"));
    }

    @Test
    public void parseConfig_success() throws Exception {
        folder.newFolder("zebedee/keyring").toPath();

        byte[] keyBytes = Base64.getDecoder().decode(TEST_SECRET_KEY);
        SecretKey expectedKey = new SecretKeySpec(keyBytes, 0, keyBytes.length, "AES");


        byte[] vectorBytes = Base64.getDecoder().decode(TEST_INIT_VECTOR);
        IvParameterSpec expectedIV = new IvParameterSpec(vectorBytes);

        Config cfg = parser.parseConfig(
                TEST_SECRET_KEY, TEST_INIT_VECTOR, TEST_SECRET_KEY, TEST_INIT_VECTOR, zebedeeRoot.toString());

        assertThat(expectedKey, equalTo(cfg.getKey()));
        assertThat(expectedIV.getIV(), equalTo(cfg.getIv().getIV()));
        assertThat(zebedeeRoot.resolve("keyring"), equalTo(cfg.getKeyringDir()));
    }

}
