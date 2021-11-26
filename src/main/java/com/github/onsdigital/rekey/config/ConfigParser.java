package com.github.onsdigital.rekey.config;

import com.github.onsdigital.rekey.RekeyException;

/**
 * Parse in the input values.
 */
public interface ConfigParser {

    /**
     * Parse the input parameters.
     *
     * @param keyStr     The current keyring encryption key as a Base64 encoded string.
     * @param ivStr      The current keyring encryption init vector as a Base64 encoded string.
     * @param newKeyStr  The new keyring encryption key to use as a Base64 encoded string.
     * @param newIvStr   The new keyring encryption init vector as a Base64 encoded string.
     * @param zebedeeDir The Zebedee root dir.
     * @return
     * @throws RekeyException
     */
    Config parseConfig(String keyStr, String ivStr, String newKeyStr, String newIvStr, String zebedeeDir) throws RekeyException;
}
