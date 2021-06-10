ALMA
====

ALMA Science Archive web services


Docker deployment
-----------------

For ease, rename one of these to just `docker-compose.yml`, or run with `docker-compose -f <file>`.

docker-compose-nossl.yml
~~~~~~~~~~~~~~~~~~

A set of HTTP-only set of running services relying on an existing Oracle database.  See the
[alma-tap.properties](blob/master/obscore/docker/config/alma-tap.properties) for details on specifying the Oracle
database credentials and location.  Port 80 is open.

docker-compose-ssl.yml
~~~~~~~~~~~~~~~~~~~~~~

A set of HTTPS-only set of running services relying on an existing Oracle database.  This requires a
volume called `server_certs` containing `server.crt` and `server.key` to enable Traefik's SSL engine.  Ports 80 and 443
 are open.

Provide a `.env` file with the following variables:
+-------------------+-------------------------+
| Key               | Description             |
+===================+=========================+
| PROXY_HOST        | Main registry hostname. |
+-------------------+-------------------------+
| DATABASE_USER     | Oracle user.            |
+-------------------+-------------------------+
| DATABASE_PASSWORD | Oracle password.        |
+-------------------+-------------------------+
| DATABASE_HOST     | Oracle host.            |
+-------------------+-------------------------+
| DATABASE_NAME     | Oracle database name.   |
+-------------------+-------------------------+


reg
~~~

The Registry service to distribute URLs for running services, namely the SIA and ObsCore services.


sia
~~~

SIA v2 service.  This uses the ObsCore (TAP) service as described by the registry.  See the `SiaRunner.properties` 
file to specify the TAP URI to use.

obscore
~~~~~~~

ObsCore (TAP) service to query the ALMA ObsCore database.  Use and existing Oracle database, or run the Oracle 11g XE
 Docker image.


datalink
~~~~~~~~~

DataLink service to expand an MOUS ID into download URLs

data
~~~~

Internal service to run on a storage (NGAS) node and execute the cutout code directly against files.

soda
~~~~

IVOA SODA service.
