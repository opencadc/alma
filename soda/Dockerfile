FROM opencadc/alma-tomcat:9-jdk11-openjdk-slim

RUN rm -rf webapps/*

COPY build/libs/*.war webapps/
