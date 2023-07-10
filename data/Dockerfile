FROM tomcat:9-jdk17-openjdk-slim as build

RUN apt-get update \
    && apt-get install -y bzip2 curl gcc make sudo

ADD "https://launchpad.net/ubuntu/+archive/primary/+sourcefiles/wcslib/5.15-1/wcslib_5.15.orig.tar.bz2" "/tmp/wcslib.tar.bz2"

RUN cd /tmp \
    && tar jxvf wcslib.tar.bz2 \
    && cd wcslib-5.15 \
    && ./configure --prefix=/usr \
    && make \
    && make install

FROM tomcat:9-jdk17-openjdk-slim

RUN rm -rf webapps/*

COPY --from=build /usr/include/wcslib-5.15 /usr/include/wcslib-5.15
COPY --from=build /usr/lib/libwcs-5.15.a /usr/lib/
COPY --from=build /usr/lib/libwcs.a /usr/lib/
COPY --from=build /usr/lib/libwcs.so /usr/lib/
COPY --from=build /usr/lib/libwcs.so.5 /usr/lib/
COPY --from=build /usr/lib/libwcs.so.5.15 /usr/lib/

# Create tomcat user:group with known uid:gid
RUN groupadd --gid 8675309 tomcat \
    && useradd --uid 8675309 --home-dir /usr/local/tomcat --shell /sbin/nologin \
        --no-create-home --no-user-group --no-log-init tomcat

# Create expected config director for all application config.
RUN mkdir -p config \
  && chown -R tomcat:tomcat /usr/local/tomcat

RUN apt update \
    && apt install -y sudo \
    && echo "tomcat ALL=(root) NOPASSWD:SETENV:/usr/sbin/update-ca-certificates" >> /etc/sudoers.d/tomcat \
    && apt clean \
    && rm -rf /var/apt/lists/*

USER tomcat

WORKDIR /usr/local/tomcat

# Disable all jar scanning for TLDs
RUN echo "tomcat.util.scan.StandardJarScanFilter.jarsToSkip=*.jar" >> conf/catalina.properties

# Use the x-forwarded-xxx headers from reverse proxies.
RUN sed -i 's| \
  </Host>| \
    <Valve className="org.apache.catalina.valves.RemoteIpValve" />\r\n \
     </Host>|' \
  conf/server.xml

COPY build/libs/*.war webapps/
