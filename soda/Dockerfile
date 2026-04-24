FROM tomcat:9-jdk21
ARG TOMCAT_USER=tomcat

# APT over HTTPS (restricted networks) and sudo so the runtime user can refresh the
# trust store after adding PEMs under /usr/local/share/ca-certificates/.
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
    apt-get install --no-install-recommends -y sudo; \
    rm -rf /var/lib/apt/lists/*

# Create tomcat user:group
RUN groupadd ${TOMCAT_USER} \
    && useradd --home-dir /usr/local/tomcat --shell /sbin/nologin \
        --no-create-home --no-user-group --no-log-init ${TOMCAT_USER}

RUN printf '%s\n' "${TOMCAT_USER} ALL=(root) NOPASSWD: /usr/sbin/update-ca-certificates" \
        > /etc/sudoers.d/tomcat-update-ca \
    && chmod 0440 /etc/sudoers.d/tomcat-update-ca \
    && visudo -cf /etc/sudoers.d/tomcat-update-ca

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
