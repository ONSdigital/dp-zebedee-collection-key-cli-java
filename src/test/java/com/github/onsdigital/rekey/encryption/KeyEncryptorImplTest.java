package com.github.onsdigital.rekey.encryption;

import com.github.onsdigital.rekey.files.FilesHelperImpl;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;

import static junit.framework.TestCase.assertTrue;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;

public class KeyEncryptorImplTest {

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    private Path keyringDir;
    private Path keyFile;

    @Before
    public void setUp() throws Exception {
        this.keyringDir = folder.newFolder("keyring").toPath();
        this.keyFile = keyringDir.resolve("abc123.txt");
    }

    @After
    public void tearDown() throws Exception {
        folder.delete();
    }

    @Test
    public void testEncrypt() throws Exception {
        SecretKey key = newSecretKey();
        SecretKey collectionKey = newSecretKey();
        IvParameterSpec initV = newIV();

        List<CollectionKey> input = new ArrayList<CollectionKey>() {{
            add(new CollectionKey(collectionKey, "abc123"));
        }};

        KeyEncryptor encryptor = new KeyEncryptorImpl();
        encryptor.encryptToFile(input, keyringDir, key, initV);

        assertTrue(Files.exists(keyFile));

        KeyDecryptor decryptor = new KeyDecryptorImpl(new FilesHelperImpl(), (p) -> true);
        List<CollectionKey> encryptedKeys = decryptor.decreptKeys(keyringDir, key, initV);

        assertThat(encryptedKeys.size(), equalTo(1));
        assertThat(encryptedKeys.get(0).getCollectionID(), equalTo("abc123"));
        assertThat(encryptedKeys.get(0).getKey(), equalTo(collectionKey));
    }

    private IvParameterSpec newIV() {
        byte[] iv = new byte[16];
        SecureRandom random = new SecureRandom();
        random.nextBytes(iv);
        return new IvParameterSpec(iv);
    }

    private SecretKey newSecretKey() throws Exception {
        return KeyGenerator.getInstance("AES").generateKey();
    }
}
