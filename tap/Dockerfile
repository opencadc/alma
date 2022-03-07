FROM opencadc/alma-tomcat:9-openjdk11-slim

RUN rm -rf webapps/*

COPY LocalAuthority.properties /root/config/
COPY build/libs/*.war webapps/
