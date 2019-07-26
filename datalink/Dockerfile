FROM opencadc/tomcat:8.5-jdk11-slim

RUN rm -rf webapps/* \
    && mkdir -p /root/config

COPY build/libs/*.war webapps/
