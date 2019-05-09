FROM opencadc/tomcat:alpine

RUN rm -rf webapps/*

COPY *.properties /root/config/
COPY *.war webapps/
