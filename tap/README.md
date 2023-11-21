# ALMA TAP (2.3.0)

[IVOA TAP](http://ivoa.net/Documents/TAP/) service for the [ALMA Science Archive](https://almascience.org).

## Build

The shared library, `alma-lib` needs to be built first.
With Gradle >= 6, from `alma.git/alma-lib`:

```sh
$ gradle --info clean build test publishToMavenLocal
```

Building the service creates a WAR artifact. From the root fo the
`alma.git/tap` folder, run:

```sh
$ gradle --info clean build test
```

to create the `build/libs/tap.war` file.

## Configuration

### Database

This is a working prototype using a TAP implementation with an Oracle 19c database.  A database pool will be created by
the service.  It will expect System Properties to be set to find the database.

```sh
$ ... -Dalma.tapuser.username=searchuser -Dalma.tapuser.password=searchuserpwd -Dalma.tapuser.driverClassName=oracle.jdbc.OracleDriver -Dalma.tapuser.url=jdbc:oracle:thin:@oracle-host/ALMADB
```

| Property name                       | Purpose                                                                          | Example                                  |
|-------------------------------------|----------------------------------------------------------------------------------|------------------------------------------|
| `alma.tapuser.driverClassName`      | The JDBC Driver Java class name for TAP queries                                  | oracle.jdbc.OracleDriver                 |
| `alma.tapuser.username`             | The database username for login for TAP queries                                  | almauser                                 |
| `alma.tapuser.password`             | The database password for login for TAP queries                                  | almapasswd                               |
| `alma.tapuser.url`                  | The JDBC URL for connecting for TAP queries                                      | jdbc:oracle:thin:@host:1521/DATABASE_SID |
| `alma.tapuser.maxConnections`       | The maximum number of connections for TAP queries                                | 4                                        |
| `alma.tapuser.maxWaitMilliseconds`  | The maximum number of milliseconds to wait for a new connection for TAP queries  | 20000                                    |
| `alma.tapuploadadm.driverClassName`      | The JDBC Driver Java class name for TAP uploads                                  | oracle.jdbc.OracleDriver                 |
| `alma.tapuploadadm.username`             | The database username for login for TAP uploads                                  | almauser                                 |
| `alma.tapuploadadm.password`             | The database password for login for TAP uploads                                  | almapasswd                               |
| `alma.tapuploadadm.url`                  | The JDBC URL for connecting for TAP uploads                                      | jdbc:oracle:thin:@host:1521/DATABASE_SID |
| `alma.tapuploadadm.maxConnections`       | The maximum number of connections for TAP uploads                                | 4                                        |
| `alma.tapuploadadm.maxWaitMilliseconds`  | The maximum number of milliseconds to wait for a new connection for TAP uploads  | 20000                                    |

### Asynchronous Queries

The TAP service supports asynchronous queries using POST with the /async endpoint, as well as a file upload option.  This is all stored
temporarily on disk using the [`cadc-tap-tmp`](https://github.com/opencadc/tap/tree/master/cadc-tap-tmp) library.  This library expects to find the `cadc-tap-tmp.properties` file in the `/usr/local/tomcat/config` directory and should contain two entries:

| Property name                     | Purpose                                     | Example                          |
| --------------------------------- | ------------------------------------------- | -------------------------------- |
| `org.opencadc.tap.baseStorageDir` | A writeable location on disk to store files | `/tmp`                           |
| `org.opencadc.tap.baseURL`        | The base URL to issue GETs to obtain the stored files later.  This will usually be the base TAP URL.  This URL will also have the `/files` endpoint appended to it.    | `https://almascience.org/tap`    |

## Deployment

### Docker

See [Configuration](#configuration) for database options.

```sh
docker pull opencadc/alma-tap:2.3.0
```

### Building for Docker

After the [Build](#build) step above, we can create a Docker deployment like so:

```sh
$ docker build -t opencadc/alma-tap:2.3.0 .
```

```sh
docker run --name tap -p 8080:8080 -e CATALINA_OPTS="-Dalma.tapuser.driverClassName=org.postgres.jdbc.Driver -D..." opencadc/alma-tap:2.3.0
```

The necessary Docker images will be downloaded, including the large
Oracle one, then the service will be available on port `8080`. You can
then issue a request like:

http://localhost:8080/tap/availability

Which will provide you with an XML document as to the health of the
service. If it reads with the message:

```sh
  The TAP ObsCore service is accepting queries
```

Then the TAP service is running properly. You can then issue a query to
the sample ObsCore table:

```sh
$ curl -L -d 'QUERY=SELECT+TOP+1+*+FROM+TAP_SCHEMA.obscore&LANG=ADQL' http://localhost:8080/tap/sync
```

### Dedicated web server

If you have a dedicated Servlet Container (i.e. [Tomcat](http://tomcat.apache.org)) running
already, run the [Build](#build) step above, then copy the WAR artifact from
`build/libs/` to your Servlet Container's webapp deployment directory.
