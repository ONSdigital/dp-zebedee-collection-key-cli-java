package com.github.onsdigital.rekey.config;

import com.github.onsdigital.rekey.RekeyException;
import org.apache.commons.lang3.StringUtils;

import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Base64;
import java.util.Date;

import static java.text.MessageFormat.format;

public class ConfigParserImpl implements ConfigParser {

    static DateFormat dateFormat = new SimpleDateFormat("yy-MM-dd-HHmmssSSS");

    @Override
    public Config parseConfig(String keyStr,  String ivStr, String newKeyStr, String newIvStr, String zebedeeDir)
            throws RekeyException {
        SecretKey key = parseKey(keyStr);
        IvParameterSpec iv = parseIV(ivStr);

        SecretKey newKey = parseKey(newKeyStr);
        IvParameterSpec newIv = parseIV(newIvStr);

        Path zebedeeRoot = parseZebedeeRootPath(zebedeeDir);
        Path keyringDir = parseKeyringPath(zebedeeRoot);
        Path keyringBackDir = getKeyringBackUpPath(zebedeeRoot);

        return new Config(key, iv, newKey, newIv, zebedeeRoot, keyringDir, keyringBackDir);
    }

    private SecretKey parseKey(String keyStr) throws RekeyException {
        if (StringUtils.isEmpty(keyStr)) {
            throw new RekeyException("secret key value required but was null/empty");
        }

        byte[] keyBytes;
        try {
            keyBytes = Base64.getDecoder().decode(keyStr);
        } catch (Exception ex) {
            throw new RekeyException("error Base64 decoding secret key string value", ex);
        }

        SecretKey secretKey = new SecretKeySpec(keyBytes, 0, keyBytes.length, "AES");
        Arrays.fill(keyBytes, (byte) 0);
        return secretKey;
    }

    private IvParameterSpec parseIV(String ivStr) throws RekeyException {
        if (StringUtils.isEmpty(ivStr)) {
            throw new RekeyException("init vector value required but was null/empty");
        }

        byte[] ivBytes;
        try {
            ivBytes = Base64.getDecoder().decode(ivStr);
        } catch (Exception ex) {
            throw new RekeyException("error Base64 decoding init vector string value", ex);
        }

        IvParameterSpec ivParameterSpec = new IvParameterSpec(ivBytes);
        Arrays.fill(ivBytes, (byte) 0);
        return ivParameterSpec;
    }

    private Path parseZebedeeRootPath(String dir) throws RekeyException {
        if (StringUtils.isEmpty(dir)) {
            throw new RekeyException("zebedee_root dir value required but was null/empty");
        }

        return validateDir(Paths.get(dir));
    }

    private Path parseKeyringPath(Path zebedeeDir) throws RekeyException {
        return validateDir(zebedeeDir.resolve("keyring"));
    }

    private Path getKeyringBackUpPath(Path zebedeeDir) {
        String backupDir = format("keyring-backup-{0}", dateFormat.format(new Date()));
        return zebedeeDir.resolve(backupDir);
    }

    private Path validateDir(Path p) throws RekeyException {
        if (Files.notExists(p)) {
            throw new RekeyException(format("{0} dir required but does not exist", p.toString()));
        }

        if (!Files.isDirectory(p)) {
            throw new RekeyException(format("{0} is a file but expected directory", p.toString()));
        }

        return p;
    }
}
