package com.github.onsdigital.rekey.encryption;

import javax.crypto.SecretKey;
import java.nio.file.Path;

import static java.text.MessageFormat.format;

public class CollectionKey {

    private SecretKey key;
    private String collectionID;

    public CollectionKey(final SecretKey key, final String collectionID) {
        this.key = key;
        this.collectionID = collectionID;
    }

    public SecretKey getKey() {
        return this.key;
    }

    public String getCollectionID() {
        return this.collectionID;
    }

    public Path getFilename(Path keyringDir) {
        return keyringDir.resolve(format("{0}.txt", collectionID));
    }
}
