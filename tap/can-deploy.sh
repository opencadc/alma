#!/bin/bash

# Only push the Docker image for a true Production build.
if [[ "${TRAVIS_BRANCH}" = "master" && "${TRAVIS_PULL_REQUEST}" = "false" ]];
then
    echo "DEPLOY";
fi
