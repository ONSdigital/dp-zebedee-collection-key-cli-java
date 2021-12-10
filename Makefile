# Replace the placeholder values below as required.

## The current Zebedee keyring secrets.
CURRENT_KEY=<current_key_as_base64_encoded_string>
CURRENT_IV=<current_init_vector_as_base64_encoded_string>

# The new keyring secrets to migrate to. Use the /zebedee/collection-keyring-secrets-generator tool to generate a new
# set of Secret Key/Init Vector values.
NEW_KEY=<new_key_as_base64_encoded_string>
NEW_IV=<new_init_vector_as_base64_encoded_string>

## The Zebedee root dir
ZEBEDEE_ROOT=<path_to_zebedee_root_dir>

.PHONY: all
all: build test audit rekey

.PHONY: test
test:
	mvn clean test -Dossindex.skip

.PHONY: audit
audit:
	mvn ossindex:audit

.PHONY: build
build:
	mvn clean package -Dmaven.test.skip -Dossindex.skip=true

.PHONY: rekey
rekey:
	java -jar target/rekey.jar \
	-k=${CURRENT_KEY} \
	-i=${CURRENT_IV} \
 	-k2=${NEW_KEY} \
 	-i2=${NEW_IV} \
 	-z=${ZEBEDEE_ROOT} \
