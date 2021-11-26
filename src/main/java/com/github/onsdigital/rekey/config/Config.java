package com.github.onsdigital.rekey.config;

import org.apache.commons.lang3.builder.ToStringBuilder;

import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;

import static java.text.MessageFormat.format;

public class Config {

    private SecretKey key;
    private IvParameterSpec iv;
    private SecretKey newKey;
    private IvParameterSpec newIV;
    private Path zebedeeRoot;
    private Path keyringDir;
    private Path keyringBackupDir;
    private Path keyringBackupTar;

    public Config(SecretKey key, IvParameterSpec iv, SecretKey newKey, IvParameterSpec newIv, Path zebedeeRoot,
                  Path keyringDir, Path keyringBackupDir) {
        this.key = key;
        this.iv = iv;
        this.newKey = newKey;
        this.newIV = newIv;
        this.zebedeeRoot = zebedeeRoot;
        this.keyringDir = keyringDir;
        this.keyringBackupDir = keyringBackupDir;
        this.keyringBackupTar = Paths.get(format("{0}.tar.gz", keyringBackupDir.toString()));
    }

    public SecretKey getKey() {
        return this.key;
    }

    public IvParameterSpec getIv() {
        return this.iv;
    }

    public SecretKey getNewKey() {
        return this.newKey;
    }

    public IvParameterSpec getNewIV() {
        return this.newIV;
    }

    public Path getZebedeeRoot() {
        return this.zebedeeRoot;
    }

    public Path getKeyringDir() {
        return this.keyringDir;
    }

    public Path getKeyringBackupDir() {
        return this.keyringBackupDir;
    }

    public Path getKeyringBackupTar() {
        return this.keyringBackupTar;
    }

    public Path getCollectionKeyPath(File f) {
        return getKeyringDir().resolve(f.getName());
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append("key", key)
                .append("iv", iv)
                .append("newKey", newKey)
                .append("newIV", newIV)
                .append("zebedeeRoot", zebedeeRoot)
                .append("keyringDir", keyringDir)
                .append("keyringBackup", keyringBackupDir)
                .append("keyringBackupTar", keyringBackupTar)
                .toString();
    }
}
