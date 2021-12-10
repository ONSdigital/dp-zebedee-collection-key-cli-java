package com.github.onsdigital.rekey.encryption;

import com.github.onsdigital.rekey.RekeyException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.crypto.Cipher;
import javax.crypto.CipherOutputStream;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import java.io.FileOutputStream;
import java.nio.file.Path;
import java.util.List;

import static java.text.MessageFormat.format;

public class KeyEncryptorImpl implements KeyEncryptor {

    private static final Logger LOG = LogManager.getLogger(KeyEncryptorImpl.class);

    static final String CIPHER_ALGORITHM = "AES/CBC/PKCS5Padding";
    static final String ENCRYPTION_ALGORITHM = "AES";

    @Override
    public void encryptToFile(List<CollectionKey> toEncrypt, Path dest, SecretKey encryptionKey,
                              IvParameterSpec encryptionIV)
            throws RekeyException {

        LOG.info("encrypting collection keys with new secret key");
        for (CollectionKey key : toEncrypt) {
            encryptToFile(key.getKeyPath(dest), key.getKey(), encryptionKey, encryptionIV);
        }

        LOG.info("re-encryp collection keys completed successfully, total: {}", toEncrypt.size());
    }

    private void encryptToFile(Path dest, SecretKey toEncrypt, SecretKey encryptionKey, IvParameterSpec encryptionIV)
            throws RekeyException {
        try (
                FileOutputStream fos = new FileOutputStream(dest.toFile());
                CipherOutputStream cos = new CipherOutputStream(fos, getEncryptCipher(encryptionKey, encryptionIV))
        ) {
            cos.write(toEncrypt.getEncoded());
            cos.flush();
        } catch (Exception ex) {
            throw new RekeyException(format("error re-encryption collection key: {0}", dest.toString()), ex);
        }
    }

    private Cipher getEncryptCipher(SecretKey newKey, IvParameterSpec newIv) throws RekeyException {
        try {
            Cipher cipher = Cipher.getInstance(CIPHER_ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, newKey, newIv);
            return cipher;
        } catch (Exception ex) {
            throw new RekeyException("error creating encryption cypher", ex);
        }
    }

    private Path getFilename(Path keyringDir, String collectionId) {
        return keyringDir.resolve(collectionId + ".txt");
    }
}
