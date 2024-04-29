FROM tomcat:9-jdk17

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
