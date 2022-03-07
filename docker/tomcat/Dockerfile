FROM tomcat:9-jdk11-openjdk-slim

RUN sed -i 's| \
  </Host>| \
    <Valve className="org.apache.catalina.valves.RemoteIpValve" />\r\n \
     </Host>|' \
  /usr/local/tomcat/conf/server.xml

CMD [ "catalina.sh", "run" ]
