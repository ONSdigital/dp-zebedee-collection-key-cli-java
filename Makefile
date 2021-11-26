# Replace the placeholder values below.
CURRENT_KEY=<current_key_as_base64_encoded_string>
CURRENT_IV=<current_init_vector_as_base64_encoded_string>
NEW_KEY=<new_key_as_base64_encoded_string>
NEW_IV=<new_init_vector_as_base64_encoded_string>
ZEBEDEE_ROOT=<path_to_zebedee_root_dir>

.PHONY: all
all: build rekey

.PHONY: build
build:
	mvn clean package -DskipTests

.PHONY: rekey
rekey:
	java -jar target/dp-zebedee-collection-key-cli-java-1.0-SNAPSHOT-jar-with-dependencies.jar \
	--key=${CURRENT_KEY} \
	--iv=${CURRENT_IV} \
 	--new-key=${NEW_KEY} \
 	--new-iv=${NEW_IV} \
 	--zebedee-root=${ZEBEDEE_ROOT} \
