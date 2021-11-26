package com.github.onsdigital.rekey.encryption;

import com.github.onsdigital.rekey.RekeyException;

import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import java.nio.file.Path;
import java.util.List;

public interface KeyEncryptor {

    void encryptToFile(List<CollectionKey> toEncrypt, Path dest, SecretKey encryptionKey, IvParameterSpec encryptionIV)
            throws RekeyException;
}
