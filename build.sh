#!/bin/bash

PROJECT_DIR=$(pwd)

cd ${PROJECT_DIR}/alma-lib && gradle -i clean test publishToMavenLocal

for i in datalink obscore reg sia soda tap;
do
  cd ${PROJECT_DIR}/${i} && gradle -i clean build test;
  VERSION=`cat ../properties.gradle| grep -e "^version" | awk -F \' '{print $2}'`;
  echo "**";
  echo "** Building opencadc/alma-${i}:${VERSION}";
  echo "**";
  docker build -t opencadc/alma-${i}:${VERSION} .;
done
