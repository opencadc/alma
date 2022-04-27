FROM tomcat:9-jdk11-openjdk-slim as build

RUN apt-get update \
    && apt-get install -y bzip2 curl gcc make

ADD "https://launchpad.net/ubuntu/+archive/primary/+sourcefiles/wcslib/5.15-1/wcslib_5.15.orig.tar.bz2" "/tmp/wcslib.tar.bz2"

RUN cd /tmp \
    && tar jxvf wcslib.tar.bz2 \
    && cd wcslib-5.15 \
    && ./configure --prefix=/usr \
    && make \
    && make install

FROM opencadc/proxy-tomcat:9-jdk11-openjdk-slim

RUN rm -rf webapps/*

COPY --from=build /usr/include/wcslib-5.15 /usr/include/wcslib-5.15
COPY --from=build /usr/lib/libwcs-5.15.a /usr/lib/
COPY --from=build /usr/lib/libwcs.a /usr/lib/
COPY --from=build /usr/lib/libwcs.so /usr/lib/
COPY --from=build /usr/lib/libwcs.so.5 /usr/lib/
COPY --from=build /usr/lib/libwcs.so.5.15 /usr/lib/

COPY build/libs/*.war webapps/
