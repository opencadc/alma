#!/bin/bash
set -euo pipefail

TOMCAT_USER="${TOMCAT_USER:-tomcat}"

if compgen -G "/usr/local/share/ca-certificates/*.crt" > /dev/null; then
    /usr/local/sbin/update-trust-stores.sh
fi

if [[ "$(id -u)" -eq 0 ]]; then
    exec runuser -u "${TOMCAT_USER}" -w /usr/local/tomcat -- "${@}"
fi

exec "${@}"
