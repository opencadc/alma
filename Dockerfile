FROM gradle:4.10-jdk11-slim

USER root

RUN apt-get update && \
    apt-get install -y python3 python3-pip git

RUN ln -s /usr/bin/python3 /usr/local/bin/python

RUN pip3 install doc8

USER gradle
