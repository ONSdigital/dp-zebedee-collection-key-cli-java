# dp-zebedee-collection-keyring-cli-java
Java based cli for managing Zebedee collection encryption keys

## Rekey

If the Zebedee Keyring encryption key is ever exposed or compremised **immediate action** is required minimise the risk 
and window of opportunity for malicious parties. The `rekey` command enables us to can programatically swap 
encryption keys. 

The `rekey` command will:
- Create a `tar.gz` back up of the current `zebedee/keyring` dir in case a rollback is required.
- Decrypt each key file using the **current key**, re-encrypt it with the **new key** and write the output back to 
  the keyring dir.

## Rekey guide.
TODO




