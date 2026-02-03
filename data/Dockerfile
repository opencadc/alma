FROM tomcat:9-jdk21 AS build

RUN apt-get update \
    && apt-get install --no-install-recommends -y bzip2 curl gcc libc-dev libtool make sudo

ADD "https://launchpad.net/ubuntu/+archive/primary/+sourcefiles/wcslib/5.15-1/wcslib_5.15.orig.tar.bz2" "/tmp/wcslib.tar.bz2"

RUN cd /tmp \
    && tar jxvf wcslib.tar.bz2 \
    && cd wcslib-5.15 \
    && ./configure --prefix=/usr \
    && make \
    && make install

FROM tomcat:9-jdk21

RUN rm -rf webapps/*

COPY --from=build /usr/include/wcslib-5.15 /usr/include/wcslib-5.15
COPY --from=build /usr/lib/libwcs-5.15.a /usr/lib/
COPY --from=build /usr/lib/libwcs.a /usr/lib/
COPY --from=build /usr/lib/libwcs.so /usr/lib/
COPY --from=build /usr/lib/libwcs.so.5 /usr/lib/
COPY --from=build /usr/lib/libwcs.so.5.15 /usr/lib/

# Allow it to be set at build time.
ARG TOMCAT_USER=tomcat

# Create tomcat user:group
RUN groupadd ${TOMCAT_USER} \
    && useradd --home-dir /usr/local/tomcat --shell /sbin/nologin \
        --no-create-home --no-user-group --no-log-init ${TOMCAT_USER}

# Create expected config director for all application config.
RUN mkdir -p config \
  && chown -R tomcat:tomcat /usr/local/tomcat

USER ${TOMCAT_USER}

WORKDIR /usr/local/tomcat

# Disable all jar scanning for TLDs
RUN echo "tomcat.util.scan.StandardJarScanFilter.jarsToSkip=*.jar" >> conf/catalina.properties

# Use the x-forwarded-xxx headers from reverse proxies.
RUN sed -i 's| \
  </Host>| \
    <Valve className="org.apache.catalina.valves.RemoteIpValve" />\r\n \
     </Host>|' \
  conf/server.xml

RUN rm -rf webapps/*

COPY build/libs/*.war webapps/
