package com.github.onsdigital.rekey.acceptance;

import com.github.onsdigital.rekey.RekeyComand;
import com.github.onsdigital.rekey.config.ConfigParser;
import com.github.onsdigital.rekey.config.ConfigParserImpl;
import com.github.onsdigital.rekey.encryption.KeyDecryptor;
import com.github.onsdigital.rekey.encryption.KeyDecryptorImpl;
import com.github.onsdigital.rekey.encryption.KeyEncryptor;
import com.github.onsdigital.rekey.encryption.KeyEncryptorImpl;
import com.github.onsdigital.rekey.files.FileArchiver;
import com.github.onsdigital.rekey.files.FileArchiverImpl;
import com.github.onsdigital.rekey.files.FilesHelper;
import com.github.onsdigital.rekey.files.FilesHelperImpl;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.CipherOutputStream;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.function.Predicate;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;

public class RekeyAcceptanceTest {

    static final String ENCRYPTION_ALGORITHM = "AES";
    static final String CIPHER_ALGORITHM = "AES/CBC/PKCS5Padding";
    static final String CLEAR_TEXT = "Yippi Kai Ya";

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    private RekeyComand rekey;
    private Path zebedeeDir;
    private Path keyringDir;
    private Path keyFile;
    private SecretKey collectionKey, key1, key2;
    private IvParameterSpec collectionIV, iv1, iv2;

    @Before
    public void setUp() throws Exception {
        this.zebedeeDir = folder.newFolder("zebedee").toPath();
        this.keyringDir = folder.newFolder("zebedee/keyring").toPath();
        this.keyFile = folder.newFile("zebedee/keyring/testcollection.txt").toPath();

        this.collectionKey = newSecretKey();
        this.key1 = newSecretKey();
        this.key2 = newSecretKey();

        this.collectionIV = newIV();
        this.iv1 = newIV();
        this.iv2 = newIV();

        ConfigParser cfgParse = new ConfigParserImpl();
        FilesHelper filesHelper = new FilesHelperImpl();
        FileArchiver archiver = new FileArchiverImpl();
        Predicate<Path> keyFilesFilter = (p) -> Files.isRegularFile(p);
        KeyDecryptor decryptor = new KeyDecryptorImpl(filesHelper, keyFilesFilter);
        KeyEncryptor encryptor = new KeyEncryptorImpl();

        this.rekey = new RekeyComand(cfgParse, decryptor, encryptor, archiver, filesHelper, keyFilesFilter);

        FieldUtils.writeField(rekey, "key", Base64.getEncoder().encodeToString(key1.getEncoded()), true);
        FieldUtils.writeField(rekey, "iv", Base64.getEncoder().encodeToString(iv1.getIV()), true);

        FieldUtils.writeField(rekey, "newKey", Base64.getEncoder().encodeToString(key2.getEncoded()), true);
        FieldUtils.writeField(rekey, "newIv", Base64.getEncoder().encodeToString(iv2.getIV()), true);

        FieldUtils.writeField(rekey, "zebedeeDir", zebedeeDir.toString(), true);
    }

    @After
    public void tearDown() throws Exception {
        folder.delete();
    }

    /**
     * Test verifies that the rekey command works as expected:
     *
     * - Given the keyring dir contains a key file encrypted with Secret Key 1.
     *
     *   - When the Rekey command is invoked.
     *
     *     - Then the key file is reencrypted with Secret Key 2.
     *
     *     - And the reencrypted key file replaces the original key file.
     *
     *     - And reencrypted key file can be decrypted with Secret Key 2.
     */
    @Test
    public void testRekeyCommand() throws Exception {
        // Encrypt a plain text message with a Secret Key and assign the encrypted bytes to a var for later.
        byte[] cipherText = encryptBytes(CLEAR_TEXT.getBytes(StandardCharsets.UTF_8), collectionKey, collectionIV);

        // Encrypt the key used above with another Secret Key (Key 1) & write the output to a key file in the keyring
        // dir.
        encryptAndWriteToFile(keyFile.toFile(), collectionKey.getEncoded(), key1, iv1);

        // Run the rekey command.
        // This will decrypt each key file in the keyring dir using Key 1, re-encrypt the content with the new key
        // (Key 2) and write the encyrpted bytes to a key file in the keyring dir replacing the original src key file.
        rekey.call();

        // Decrypt the key file using the new key (key2).
        byte[] fileBytes = readEncryptedFile(keyFile.toFile(), key2, iv2);

        // Construct a SecretKey object from the decrypted key file bytes.
        SecretKey decryptedKey = new SecretKeySpec(fileBytes, 0, fileBytes.length, ENCRYPTION_ALGORITHM);
        assertThat(decryptedKey.getEncoded(), equalTo(collectionKey.getEncoded()));

        // Attempt to decrypt our original cipher text with the decrypted key value.
        byte[] decryptedBytes = decryptBytes(cipherText, decryptedKey, collectionIV);

        // Convert the decrypted cipher text bytes to a string and compare to our original plain text message - if
        // successful they should match.
        String decryptedMessage = new String(decryptedBytes);
        assertThat(decryptedMessage, equalTo(CLEAR_TEXT));
    }

    private void encryptAndWriteToFile(File f, byte[] content, SecretKey key, IvParameterSpec iv) throws Exception {
        try (
                FileOutputStream fos = new FileOutputStream(f);
                CipherOutputStream cos = new CipherOutputStream(fos, getEncryptCipher(key, iv))
        ) {
            cos.write(content);
            cos.flush();
        }
    }

    private byte[] readEncryptedFile(File f, SecretKey key, IvParameterSpec iv) throws Exception {
        byte[] keyBytes = null;
        try (
                FileInputStream fin = new FileInputStream(f);
                CipherInputStream cin = new CipherInputStream(fin, getDecryptCipher(key, iv));
        ) {
            return IOUtils.toByteArray(cin);
        }
    }

    private Cipher getEncryptCipher(SecretKey key, IvParameterSpec iv) throws Exception {
        Cipher cipher = Cipher.getInstance(CIPHER_ALGORITHM);
        cipher.init(Cipher.ENCRYPT_MODE, key, iv);
        return cipher;
    }

    private Cipher getDecryptCipher(SecretKey key, IvParameterSpec iv) throws Exception {
        Cipher cipher = Cipher.getInstance(CIPHER_ALGORITHM);
        cipher.init(Cipher.DECRYPT_MODE, key, iv);
        return cipher;
    }

    private byte[] readFileBytes(File src) throws Exception {
        try (FileInputStream fin = new FileInputStream(src)) {
            return IOUtils.toByteArray(fin);
        }
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

    byte[] encryptBytes(byte[] bytes, SecretKey key, IvParameterSpec iv) throws Exception {
        Cipher cipher = Cipher.getInstance(CIPHER_ALGORITHM);
        cipher.init(Cipher.ENCRYPT_MODE, key, iv);

        return cipher.doFinal(bytes);
    }

    byte[] decryptBytes(byte[] encryptedMessage, SecretKey key, IvParameterSpec iv) throws Exception {
        Cipher cipher = Cipher.getInstance(CIPHER_ALGORITHM);
        cipher.init(Cipher.DECRYPT_MODE, key, iv);

        return cipher.doFinal(encryptedMessage);
    }

}
