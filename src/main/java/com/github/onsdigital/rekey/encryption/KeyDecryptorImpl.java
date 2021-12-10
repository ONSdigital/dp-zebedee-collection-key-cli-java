package com.github.onsdigital.rekey.encryption;

import com.github.onsdigital.rekey.RekeyException;
import com.github.onsdigital.rekey.files.FilesHelper;
import org.apache.commons.io.IOUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.FileInputStream;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Predicate;

import static java.text.MessageFormat.format;
import static org.apache.commons.io.FilenameUtils.removeExtension;

/**
 * Provides functionality for decrypting SecretKey encrypted collection key files.
 */
public class KeyDecryptorImpl implements KeyDecryptor {

    private static final Logger LOG = LogManager.getLogger(KeyDecryptorImpl.class);

    static final String CIPHER_ALGORITHM = "AES/CBC/PKCS5Padding";
    static final String ENCRYPTION_ALGORITHM = "AES";
    static final String TXT_EXT = "txt";

    private FilesHelper filesHelper;
    private Predicate<Path> keyFileFilter;

    /**
     * Construct a new instance of the KeyDecryptor
     *
     * @param filesHelper   the filehelper to use.
     * @param keyFileFilter a predicate to filter which files to decrypt.
     */
    public KeyDecryptorImpl(final FilesHelper filesHelper, Predicate<Path> keyFileFilter) {
        this.filesHelper = filesHelper;
        this.keyFileFilter = keyFileFilter;
    }

    @Override
    public List<CollectionKey> decreptKeys(Path keyringDir, SecretKey key, IvParameterSpec iv) throws RekeyException {
        List<CollectionKey> results = new ArrayList<>();

        List<Path> keyFiles = filesHelper.listFiles(keyringDir, keyFileFilter);
        LOG.info("decrypting existing collection keys (total: {})", keyFiles.size());

        for (Path p : keyFiles) {
            SecretKey k = decryptKey(p, key, iv);
            results.add(new CollectionKey(k, removeExtension(p.getFileName().toString())));
        }

        LOG.info("successfully decrypted existing collection keys (count: {})", results.size());
        return results;
    }

    private SecretKey decryptKey(Path keyFile, SecretKey key, IvParameterSpec iv) throws RekeyException {
        byte[] keyBytes = null;
        try (
                FileInputStream fin = new FileInputStream(keyFile.toFile());
                CipherInputStream cin = new CipherInputStream(fin, getDecryptCipher(key, iv))
        ) {
            keyBytes = IOUtils.toByteArray(cin);
            return new SecretKeySpec(keyBytes, 0, keyBytes.length, ENCRYPTION_ALGORITHM);
        } catch (Exception ex) {
            throw new RekeyException(format("error decrypting secret key: {0}", keyFile), ex);
        } finally {
            if (keyBytes != null) {
                Arrays.fill(keyBytes, (byte) 0);
            }
        }
    }

    private Cipher getDecryptCipher(SecretKey key, IvParameterSpec iv) throws RekeyException {
        try {
            Cipher cipher = Cipher.getInstance(CIPHER_ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, key, iv);
            return cipher;
        } catch (Exception ex) {
            throw new RekeyException("error creating decryption cypher", ex);
        }
    }
}
