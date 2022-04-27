FROM opencadc/proxy-tomcat:9-jdk11-openjdk-slim

RUN rm -rf webapps/* \
    && mkdir -p config

COPY build/libs/*.war webapps/
