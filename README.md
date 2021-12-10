# dp-zebedee-collection-keyring-cli-java
Java based cli for managing Zebedee collection encryption keys. If the Zebedee Keyring encryption key is ever exposed 
or compremised **immediate action** is required minimise the risk 
and window of opportunity for malicious parties. The `dp-zebedee-collection-keyring-cli-java` cli provides a `rekey` 
command to automate the process of swapping the keyring encryption key with minimal fuss. 

The `rekey` command will:
- Create a `tar.gz` back up of the current `zebedee/keyring` dir in case a rollback is required.
- Decrypt each key file using the **current** secret key.
- Re-encrypt each collection key with the **new secret key** and write the output back to the `/zebedee/keyring` dir.
- Verify that all keys have been re-encrypted.

## Swapping keys
(These steps assume `rekey` is being run on the prod environment).

1. Use the [collection-keyring-secrets-generator tool](1) to generate a new set of `SecretKey` and `InitVector` 
   values. The tool will output the key in the correct/required format. 

    :warning: These are now production secrets so **please ensure they are kept safe and secure** and are not 
   accidentally shared. :warning:


2. Update the following Zebedee CMS secrets in [dp-cofigs](2) with the newly generated values: 

    ```
   "KEYRING_SECRET_KEY": "<new_key_value>",
   "KEYRING_INIT_VECTOR": "<new_iv_value>",
   ```
   **Keep a copy of the original key/iv values as these will be required later**. Don't merge the PR until the rekey 
   command has run and the existing collection keys have  been re-encrypted with the new key.


3. Stop Zebedee CMS via the Nomad UI and wait for the service to be confirmed as **DEAD** before continuing. We 
   need to ensure that no new collections are created/updated while the rekey process is running.


4. `ssh` on to the publishing box (Zebedee will be on `publishing_mount 1`)


5. Create/start a new Java docker container with a volume mapped to the `zebedee_root` dir on publishing instance.
    ```bash
    docker run -i -t --name rekey -v /var/florence/zebedee/:/content openjdk:11 /bin/bash
    ```

6. Install `maven`, `git` and `make`, clone the repo and build the `rekey` jar:
    ```bash
    $> apt-get update && apt-get install maven && apt-get install git && apt-get install make
    $> git clone https://github.com/ONSdigital/dp-zebedee-collection-key-cli-java.git
    $> cd dp-zebedee-collection-key-cli-java 
    $> make build
    ```

7. Run the `rekey` command. A summary of the command flags is given below:

    ```bash
    java -jar target/rekey.jar -k="<current_key>" -i="<current_iv>" -k2="<new_key>" -i2="<new_iv>" -z="<zebedee_root_dir>"
    ```
   | Flag | Description |
   |------|-------------------------------------------------------------------------------|
   | `-k` | The current secret key value - required to decrypt the collection keys.       |
   | `-i` | The current init vector value - required to decrypt the collection keys.      |
   | `-k2`| The new secret key value to encrypt the collection keys with.                 |
   | `-i2`| The new init vector value - required to encrypt the collection keys.          |
   | `-z` | The path to the Zebedee root directory - for dev/prod this will be `/content`.|


8. If there are no errors `rekey` command has completed successfully and all collections keys 
   under `$zebedee_root/keyring` should now be encrypted with the new key.


9. Merge your dp-configs secrets PR.


10. Once the secrets pipeline has completed restart Zebedee CMS ensuring that the latest secrets have been picked up.
     To verify `rekey` has been successful login to Florence and attempt to view content in any of the existing 
     collections - If `rekey` was successful and Zebedee has the updated keyring secrets you should be able to view 
     the collection content without error.


11. Assuming everything has completed successfully - Exit the docker container and remove it:
      ```bash
      sudo docker rm rekey
      ```
   Congratuations - you have successfully completed your mission.
   

### Rolling back
Before decrypting/re-encrypting the `rekey` command will create a backup of the current keyring directory - 
`keyring-backup-<timestamp>.tar.gz`. If `rekey` is unsucessful or a rollback is required for any reason 
1. Untar the backup tar.gz:
   ````bash
   tar -xf keyring-backup-<timestamp>.tar.gz
   ````
2. Rename the output dir to `keyring`. This will replace the current `keyring` directory with the original contain the 
keys encrypted with the old key.

   ````bash
   mv keyring-backup-<timestamp> keyring
   ````
 
3. Revert the Zebedee secrets (if required).


4. Restart Zebedee (if required).

[1]: https://github.com/ONSdigital/zebedee/tree/develop/collection-keyring-secrets-generator
[2]: https://github.com/ONSdigital/dp-configs


