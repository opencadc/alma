FROM opencadc/tomcat:8.5-jdk11-slim

RUN rm -rf webapps/*

COPY docker/*.properties /root/config/
COPY build/libs/*.war webapps/
