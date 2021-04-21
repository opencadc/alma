FROM tomcat:9-jdk11-openjdk-slim

RUN rm -rf webapps/*

COPY docker/*.properties /root/config/
COPY build/libs/*.war webapps/
