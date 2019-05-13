# ALMA
ALMA Science Archive web services


## Docker deployment

### docker-compose.yml

The default HTTP-only set of running services.  It will use the supplied Docker Oracle database.  Port 80 is open.

### docker-compose-ssl.yml

The default HTTPS-only set of running services.  It will use the supplied Docker Oracle database.  This requires a 
volume called `server_certs` containing `server.crt` and `server.key` to enable NGINX's SSL engine.  Port 443 is open.

### docker-compose-no-db.yml

A set of HTTP-only set of running services relying on an existing Oracle database.  See the 
[alma-tap.properties](blob/master/obscore/docker/config/alma-tap.properties) for details on specifying the Oracle
database credentials and location.  Port 80 is open.

### docker-compose-no-db-ssl.yml

A set of HTTPS-only set of running services relying on an existing Oracle database.  This requires a 
volume called `server_certs` containing `server.crt` and `server.key` to enable NGINX's SSL engine.  Port 443 is open.


### reg

The Registry service to distribute URLs for running services, namely the SIA and ObsCore services.


### sia

SIA v2 service.  This uses the ObsCore (TAP) service as described by the registry.  See the `SiaRunner.properties` 
file to specify the TAP URI to use.

### obscore

ObsCore (TAP) service to query the ALMA ObsCore database.  Use and existing Oracle database, or run the Oracle 11g XE
 Docker image.
 
 
 
