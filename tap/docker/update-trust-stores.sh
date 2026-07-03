#!/bin/bash
set -euo pipefail

JAVA_HOME="${JAVA_HOME:-/opt/java/openjdk}"

update-ca-certificates

if [[ ! -x "${JAVA_HOME}/bin/keytool" ]]; then
    echo "Unable to locate keytool under JAVA_HOME (${JAVA_HOME})." >&2
    exit 1
fi

KEYTOOL="${JAVA_HOME}/bin/keytool"
JAVA_CACERTS="${JAVA_HOME}/lib/security/cacerts"
CA_DIR="/usr/local/share/ca-certificates"

shopt -s nullglob
custom_certs=("${CA_DIR}"/*.crt)
shopt -u nullglob

for cert in "${custom_certs[@]}"; do
    alias_name="custom-$(basename "${cert}" .crt)"
    "${KEYTOOL}" -importcert -noprompt -trustcacerts \
        -alias "${alias_name}" \
        -file "${cert}" \
        -keystore "${JAVA_CACERTS}" \
        -storepass changeit
done
