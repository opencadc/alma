FROM opencadc/proxy-tomcat:9-jdk11-openjdk-slim

RUN rm -rf webapps/*

COPY docker/*.properties config/
COPY build/libs/*.war webapps/
