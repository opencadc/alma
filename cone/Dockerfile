FROM tomcat:9-jdk21

ENV JAVA_HOME=/opt/java/openjdk
ENV PATH="${JAVA_HOME}/bin:${PATH}"

ARG TOMCAT_USER=tomcat

# APT over HTTPS for restricted networks.
RUN set -eux; \
    if [ -f /etc/apt/sources.list.d/ubuntu.sources ]; then \
      sed -i '/^URIs:/s|http://|https://|g' /etc/apt/sources.list.d/ubuntu.sources; \
    fi; \
    for f in /etc/apt/sources.list /etc/apt/sources.list.d/*.list; do \
      [ -f "$f" ] || continue; \
      sed -i \
        -e 's|^deb http://|deb https://|g' \
        -e 's|^deb-src http://|deb-src https://|g' \
        "$f"; \
    done; \
    apt-get update; \
    rm -rf /var/lib/apt/lists/*

# Create tomcat user:group
RUN groupadd ${TOMCAT_USER} \
    && useradd --home-dir /usr/local/tomcat --shell /sbin/nologin \
        --no-create-home --no-user-group --no-log-init ${TOMCAT_USER}

COPY docker/entrypoint.sh docker/update-trust-stores.sh /usr/local/sbin/
RUN chmod 755 /usr/local/sbin/entrypoint.sh /usr/local/sbin/update-trust-stores.sh

# Create expected config director for all application config.
RUN mkdir -p config \
  && chown -R "${TOMCAT_USER}:${TOMCAT_USER}" /usr/local/tomcat

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

USER root

ENTRYPOINT ["/usr/local/sbin/entrypoint.sh"]
CMD ["catalina.sh", "run"]
