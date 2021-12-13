# dp-zebedee-collection-keyring-cli-java
Java based cli for managing Zebedee collection encryption keys. If the Zebedee Keyring encryption key is ever exposed 
or compremised **immediate action** is required minimise the risk 
and window of opportunity for malicious parties. The `dp-zebedee-collection-keyring-cli-java` cli `rekey` command 
automates the process of swapping the keyring encryption key with minimal fuss. 

The `rekey` command will:
- Create a `tar.gz` back up of the current `zebedee/keyring` dir in case a rollback is required.
- Decrypt each key file using the **current** secret key.
- Re-encrypt each collection key with the **new secret key** and write the output back to the `/zebedee/keyring` dir.
- Verify that all keys have been re-encrypted.

## Rekey instructions
(These steps assume `rekey` is being run on the prod environment).

1. Use the [collection-keyring-secrets-generator tool][1] to generate a new set of `SecretKey` and `InitVector` 
   values. This tool will output the values in the format required by `rekey`. 

    :warning:
    These are now production secrets **please ensure they are kept safe and secure** and are not accidentally shared. 
   :warning:


2. Update the following Zebedee CMS secrets in [dp-configs][2] with the new values you generated in the step #1. 
   **Keep a copy of the original key/iv values as these will be required later**.

    ```
   "KEYRING_SECRET_KEY": "<new_key_value>",
   "KEYRING_INIT_VECTOR": "<new_iv_value>",
   ```
   Don't merge the PR until the rekey 
   command has run and the existing collection keys have  been re-encrypted with the new key.


3. Stop Zebedee CMS via the Nomad UI and wait for the service to be confirmed as **DEAD** before continuing - we 
   need to ensure that no new collections are created/updated while the rekey process is running.


4. `ssh` on to the publishing box (Zebedee will be on `publishing_mount 1`)


5. Create/start a new Java docker container with a volume mapped to the `zebedee_root` dir:
   ```bash
    sudo docker run -i -t --name rekey -v /var/florence/zebedee/:/content openjdk:11 /bin/bash
    ```
   **Note:** For local or other environments replace `/var/florence/zebedee/` with the appropriate zebedee root path.


6. Install the pre-requisite tools in the container, clone the repo and build the `rekey` jar.
    ```bash
    apt-get update && apt-get install maven && apt-get install git && apt-get install make
    ```
   Clone the repo:
   ```bash   
    git clone https://github.com/ONSdigital/dp-zebedee-collection-key-cli-java.git
   ```
   Build the cli jar: 
   ```bash
   cd dp-zebedee-collection-key-cli-java && make build
    ```

7. Run the `rekey` command updating the placeholders with the appropriate values. An explanation of each flag is 
   listed in the table below:

    ```bash
    java -jar target/rekey.jar -k="<current_key>" -i="<current_iv>" -k2="<new_key>" -i2="<new_iv>" -z="<zebedee_root_dir>"
    ```
   | Flag | Description |
   |------|--------------------------------------------------------------------------------------------------------|
   | `-k` | The current `KEYRING_SECRET_KEY` value - required to decrypt the collection keys.                      |
   | `-i` | The current `KEYRING_INIT_VECTOR` value - required to decrypt the collection keys.                     |
   | `-k2`| The new `KEYRING_SECRET_KEY` value to encrypt the collection keys with (generated in step #1) .        |
   | `-i2`| The new `KEYRING_INIT_VECTOR` value - required to encrypt the collection keys (generated in step #1) . |
   | `-z` | The path to the Zebedee root directory - for dev/prod this will be `/content`.                         |


8. If there are no errors and your output looks something like:
   ![Alt text](img1.png?raw=true "Optional Title") Then `rekey` has completed successfully and all  collections keys 
   under `$zebedee_root/keyring` should now be 
   encrypted with the new key.


9. Merge your dp-configs secrets PR.


10. Once the secrets pipeline has completed restart Zebedee CMS ensuring that the latest secrets have been picked up.
     To verify `rekey` has been successful: 
    - Login to Florence and attempt to view content in any of the existing 
         collections - If the collection keys have been successful mirgated to the new key you should be able to 
      view the collection content without error.


11. Assuming everything has completed successfully - Exit the docker container and remove it:
      ```bash
      sudo docker rm rekey
      ```
   Congratuations - you have successfully completed your mission. :rocket: :tada:
   

### Rolling back
Before decrypting/re-encrypting the `rekey` command will create a backup of the keyring directory - 
`keyring-backup-<timestamp>.tar.gz`. If `rekey` is unsucessful or a rollback is required for any reason 
1. Untar the backup tar.gz:
   ````bash
   tar -xf keyring-backup-<timestamp>.tar.gz
   ````
2. Rename the untar output dir to `keyring`. This will replace the current `keyring` directory with the original 
   keyring dir containing collection keys encrypted with the old key.

   ````bash
   mv keyring-backup-<timestamp> keyring
   ````
 
3. Revert the [dp-configs][1] Zebedee CMS secrets (if required) and wait until the secrets pipeline has compeleted.


4. Restart Zebedee and check that the correct secrets have been picked up.


5. To verify the rollback has been successful:

   - Login to Florence and attempt to view content in any of the existing collections - If the collection keys have 
     been successful mirgated to the new key you should be able to view the collection content without error.

[1]: https://github.com/ONSdigital/zebedee/tree/develop/collection-keyring-secrets-generator
[2]: https://github.com/ONSdigital/dp-configs


