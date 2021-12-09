package com.github.onsdigital.rekey.encryption;

import com.github.onsdigital.rekey.files.FilesHelper;
import com.github.onsdigital.rekey.files.FilesHelperImpl;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import java.nio.file.Path;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;

public class KeyDecryptorImplTest {

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    private FilesHelper filesHelper;
    private KeyDecryptor decryptor;
    private Predicate<Path> keyfileFilter;
    private Path keyringDir;
    private Path collectionKeyFile;

    @Before
    public void setUp() throws Exception {
        this.filesHelper = new FilesHelperImpl();
        this.keyfileFilter = (p) -> true;
        this.decryptor = new KeyDecryptorImpl(filesHelper, keyfileFilter);

        this.keyringDir = folder.newFolder("keyring").toPath();
    }

    @After
    public void tearDown() throws Exception {
        this.folder.delete();
    }

    @Test
    public void testDecryptKeys() throws Exception {
        SecretKey key = newSecretKey();
        SecretKey collectionKey = newSecretKey();

        IvParameterSpec initVector = getIV();

        List<CollectionKey> input = new ArrayList<CollectionKey>() {{
            add(new CollectionKey(collectionKey, "abc1234"));
        }};

        // Use the KeyEncryptor to write a key encrypted to the keyring dir.
        KeyEncryptor encryptor = new KeyEncryptorImpl();
        encryptor.encryptToFile(input, keyringDir, key, initVector);

        // Decrypt using the KeyDecryptor
        List<CollectionKey> keys = decryptor.decreptKeys(keyringDir, key, initVector);

        assertThat(keys.size(), equalTo(1));
        assertThat(keys.get(0).getKey(), equalTo(collectionKey));
    }

    private IvParameterSpec getIV() {
        byte[] iv = new byte[16];
        SecureRandom random = new SecureRandom();
        random.nextBytes(iv);
        return new IvParameterSpec(iv);
    }

    private SecretKey newSecretKey() throws Exception {
        return KeyGenerator.getInstance("AES").generateKey();
    }

}
