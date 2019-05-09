FROM opencadc/tomcat:8.5-alpine

RUN rm -rf webapps/*

COPY docker/*.properties /root/config/
COPY build/libs/*.war webapps/
