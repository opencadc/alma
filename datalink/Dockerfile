FROM tomcat:9-jdk17

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

COPY *.properties config/
COPY build/libs/*.war webapps/
