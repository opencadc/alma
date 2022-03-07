# ALMA Tomcat 9 instance

`opencadc/alma-tomcat:9-jdk11-openjdk-slim`

https://hub.docker.com/repository/docker/opencadc/alma-tomcat


This Docker image is based off the official Tomcat 9 Docker image [tomcat:9-jdk11-openjdk-slim](https://hub.docker.com/_/tomcat).


## Behind a reverse proxy
This image mirrors the official image with one difference in that it injects an additional Valve into the configuration that will look for the `x-forwarded-xxx` headers in order to properly facilitate redirects from within Tomcat.  The image can be run exactly as the official one would.
