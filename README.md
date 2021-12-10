# dp-zebedee-collection-keyring-cli-java
Java based cli for managing Zebedee collection encryption keys

## Rekey

If the Zebedee Keyring encryption key is ever exposed or compremised **immediate action** is required minimise the risk 
and window of opportunity for malicious parties. `rekey` cli is a command line tool that enables us to 
programatically swap the keyring encryption key with minimal fuss. 

The `rekey` command will:
- Create a `tar.gz` back up of the current `zebedee/keyring` dir in case a rollback is required.
- Decrypt each key file using the **current key**
- Re-encrypt each collection key with the **new key** and write the output back to the keyring dir.
- Verify that all keys have been re-encrypted.

## Rekey guide.
TODO

```bash
docker run -i -t --name rekey -v /var/florence/zebedee/:/content openjdk:11 /bin/bash
```

Update and install maven and git:
```bash
$> apt-get update && apt-get install maven && apt-get install git
```
Get the code:
```bash
$> git clone https://github.com/ONSdigital/dp-zebedee-collection-key-cli-java.git
```
Build the jar:
```bash
$> cd dp-zebedee-collection-key-cli-java 
$> mvn clean package
```

Run the tool:
__Note:__ Both Key and IV values should be provided as **Base64 encoded strings**.
```bash
java -jar target/rekey.jar \
  -k="<current_key>" \
  -i="<current_iv>" \ 
  -k2="<new_key>" \
  -i2="<new_iv>" \
  -z="<path_to_zebedee_root_dir>"
```



