#!/bin/bash

PROJECT_DIR=$(pwd)

cd ${PROJECT_DIR}/alma-lib && gradle -i clean install

for i in datalink obscore reg sia soda;
do
  cd ${PROJECT_DIR}/${i} && gradle -i clean build;
  VERSION=`find build/libs -type f | head | awk -F "##" '{print $2}' | awk -F ".war" '{print $1}'`;
  echo "**";
  echo "** Building opencadc/alma-${i}:${VERSION}";
  echo "**";
  docker build -t opencadc/alma-${i}:${VERSION} .;
done
