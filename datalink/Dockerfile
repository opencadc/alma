FROM at88mph/alma-tomcat:9-openjdk11-slim

RUN rm -rf webapps/* \
    && mkdir -p /root/config

COPY build/libs/*.war webapps/
