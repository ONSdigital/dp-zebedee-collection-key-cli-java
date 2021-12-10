package com.github.onsdigital.rekey;

import com.github.onsdigital.rekey.config.Config;
import com.github.onsdigital.rekey.config.ConfigParser;
import com.github.onsdigital.rekey.config.ConfigParserImpl;
import com.github.onsdigital.rekey.encryption.CollectionKey;
import com.github.onsdigital.rekey.encryption.KeyDecryptor;
import com.github.onsdigital.rekey.encryption.KeyDecryptorImpl;
import com.github.onsdigital.rekey.encryption.KeyEncryptor;
import com.github.onsdigital.rekey.encryption.KeyEncryptorImpl;
import com.github.onsdigital.rekey.files.FileArchiver;
import com.github.onsdigital.rekey.files.FileArchiverImpl;
import com.github.onsdigital.rekey.files.FilesHelper;
import com.github.onsdigital.rekey.files.FilesHelperImpl;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import javax.crypto.SecretKey;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static java.nio.file.Files.isRegularFile;
import static java.text.MessageFormat.format;
import static org.apache.commons.compress.utils.FileNameUtils.getExtension;
import static org.apache.commons.lang3.StringUtils.joinWith;

@Command(name = "rekey", version = "Rekey v1.0.0", mixinStandardHelpOptions = true,
        description = "Re-encrypt all existing collection keys with a new secret key.")
public class RekeyComand implements Callable<Integer> {

    private static Logger LOG = LogManager.getLogger(RekeyComand.class);

    static final String VERIFICATION_ERR_FMT = "rekey verification error, expected collection keys " +
            "were missing: {0} " +
            "\n\nTo rollback these changes:" +
            "\n\t1) Untar the backup keying tar.gz: {1}" +
            "\n\t2) Rename the backup dir to \"keyring\"\n";

    @Option(names = {"-k"}, required = true, paramLabel = "<current-key>",
            description = "The current Secret Key as a Base 64 encoded string.")
    private String key;

    @Option(names = {"-i"}, required = true, paramLabel = "<current-init-vector>",
            description = "The current Init Vector as a Base 64 encoded string.")
    private String iv;

    @Option(names = {"-k2"}, required = true, paramLabel = "<new-key>",
            description = "The new Secret Key to use as a Base 64 encoded string.")
    private String newKey;

    @Option(names = {"-i2"}, required = true, paramLabel = "<new-init-vector>",
            description = "The new Init Vector to use as a Base 64 encoded string.")
    private String newIv;

    @Option(names = {"-z"}, required = true, paramLabel = "<dir>",
            description = "The CMS collection keyring directory")
    private String zebedeeDir;

    private ConfigParser parser;
    private FilesHelper filesHelper;
    private KeyDecryptor decryptor;
    private KeyEncryptor encryptor;
    private FileArchiver archiver;
    private Predicate<Path> keyFileFilter;

    /**
     * @param parser
     */
    public RekeyComand(final ConfigParser parser, KeyDecryptor decryptor, KeyEncryptor encryptor,
                       FileArchiver archiver, FilesHelper filesHelper, Predicate<Path> keyFileFilter) {
        this.parser = parser;
        this.decryptor = decryptor;
        this.encryptor = encryptor;
        this.archiver = archiver;
        this.filesHelper = filesHelper;
        this.keyFileFilter = keyFileFilter;
    }

    /**
     * Re-encrypt the Zebedee collection keys with a new {@link SecretKey}.
     *
     * <ul>
     *     <li>Creates a backup tar.gz so the keys can be reverted if necessary</li>
     *     <li>Moves the current keyring dir to a backup dir and creates a new empty keyring dir.</li>
     *     <li>Decrypts the collection keys from the backup dir using the current {@link SecretKey}</li>
     *     <li>Re-encrypts the collection keys with the new {@link SecretKey} and writes them to disk in the keyring
     *     dir</li>
     *     <li>Removes the backup keyring dir</li>
     * </ul>
     */
    @Override
    public Integer call() throws Exception {
        Config cfg = parser.parseConfig(key, iv, newKey, newIv, zebedeeDir);
        LOG.info("config parsed successfully");

        createBackup(cfg, keyFileFilter);

        List<CollectionKey> rawKeys = decryptor.decreptKeys(cfg.getKeyringBackupDir(), cfg.getKey(), cfg.getIv());
        encryptor.encryptToFile(rawKeys, cfg.getKeyringDir(), cfg.getNewKey(), cfg.getNewIV());

        // Remove the old dir as it's no longer needed (keep the tar.gz).
        filesHelper.deleteDir(cfg.getKeyringBackupDir());

        verifyComplete(rawKeys, cfg);
        LOG.info("rekey completed successfully, a backup of the original keyring dir has been created here: {}",
                cfg.getKeyringBackupTar());
        return 0;
    }

    private void createBackup(Config cfg, Predicate<Path> filter) throws RekeyException {
        // Move the current keyring dir to a backup dir.
        filesHelper.move(cfg.getKeyringDir(), cfg.getKeyringBackupDir());

        // TAR up the backup dir so we can rollback the change is necessary
        LOG.info("creating keyring back up tar.gz: {}", cfg.getKeyringBackupTar());
        archiver.createTarGz(cfg.getKeyringBackupDir(), cfg.getKeyringBackupTar(), filter);

        // Create a new empty keyring dir to write the re-encrypted keys to.
        LOG.info("creating new (empty) keyring dir: {}", cfg.getKeyringDir());
        filesHelper.createDir(cfg.getKeyringDir());
    }

    public static void main(String[] args) {
        FilesHelper filesHelper = new FilesHelperImpl();
        Predicate<Path> keyFileFilter = (p) -> isRegularFile(p) && "txt".equals(getExtension(p.toFile().getName()));
        FileArchiver archiver = new FileArchiverImpl();

        ConfigParser parser = new ConfigParserImpl();

        KeyDecryptor decryptor = new KeyDecryptorImpl(filesHelper, keyFileFilter);
        KeyEncryptor encryptor = new KeyEncryptorImpl();

        RekeyComand cmd = new RekeyComand(parser, decryptor, encryptor, archiver, filesHelper, keyFileFilter);
        int code = new CommandLine(cmd).execute(args);

        System.exit(code);
    }

    private void verifyComplete(List<CollectionKey> expected, Config cfg) throws RekeyException {
        List<String> missing = expected.stream()
                .filter(key -> !filesHelper.exists(key.getKeyPath(cfg.getKeyringDir())))
                .map(key -> key.getKeyFileName())
                .collect(Collectors.toList());

        if (missing != null && missing.size() > 0) {
            throw new RekeyException(format(VERIFICATION_ERR_FMT, joinWith(",", missing), cfg.getKeyringBackupTar(),
                    cfg.getKeyringBackupDir()));
        }
    }
}
