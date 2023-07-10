#!/bin/bash

TEST_DIRECTORIES=("cone" "data" "datalink" "sia" "soda" "tap")
HOST="${1}"

if [ "${HOST}" == "" ];
then
  echo "Usage: ${0} <registry host name> <directories?>"
  echo ""
  echo "Example: ${0} alma.example.com"
  echo ""
  exit 1
fi

## See if modules were specified
shift
REQUESTED_DIRECTORIES="${@}"

run_int_test () {
  DIR="${1}"
  echo "Running tests for ${DIR}"
  gradle -i -Dca.nrc.cadc.reg.client.RegistryClient.host="${HOST}" -b "${DIR}"/build.gradle clean build test intTest
}

if [ "${REQUESTED_DIRECTORIES}" == "" ];
then
  echo "Running all integration tests"
  echo ""
  for DIR in "${TEST_DIRECTORIES[@]}" ; do
    run_int_test "${DIR}"
  done
else
  echo "Running only on ${REQUESTED_DIRECTORIES}"
  REQUESTED_DIRECTORIES=( ${REQUESTED_DIRECTORIES[@]} )
  for DIR in "${REQUESTED_DIRECTORIES[@]}" ; do
    run_int_test "${DIR}"
  done
fi
