package com.github.onsdigital.rekey.encryption;

import com.github.onsdigital.rekey.RekeyException;

import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import java.nio.file.Path;
import java.util.List;

public interface KeyDecryptor {

    List<CollectionKey> decreptKeys(Path keyringDir, SecretKey key, IvParameterSpec iv) throws RekeyException;
}
