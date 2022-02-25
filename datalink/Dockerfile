FROM tomcat:9-jdk11-openjdk-slim

RUN rm -rf webapps/* \
    && mkdir -p /root/config

COPY *.properties /root/config/
COPY build/libs/*.war webapps/
