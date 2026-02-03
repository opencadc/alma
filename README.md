# ALMA IVOA Deployment
ALMA Science Archive IVOA web services.

## Service Stack Configuration
The ALMA IVOA services require a properties file to configure the various service URIs
that the services will use to locate each other.  This is done via a Registry service
that will map the service URIs to actual URLs.

The properties file, `org.opencadc.alma.properties`, is required to be in the `~/config/` folder.  This file contains the Service URIs (identifiers)
that will be looked up in the Registry to obtain an actual URL.


`org.opencadc.alma.properties`

```properties
  almaDataLinkServiceURI=ivo://almascience.org/datalink
  almaRequestHandlerServiceURI=ivo://almascience.org/requesthandler
  almaFileSodaServicePort=8080
  almaSODAServiceURI=ivo://almascience.org/soda
  almaDataPortalServiceURI=ivo://almascience.org/dataportal
  almaLoggingServiceURL=https://example.org/logging/entry
```

| Property                      | Description                              |
|--------------------------------|------------------------------------------|
| `almaDataLinkServiceURI`        | DataLink service identifier              |
| `almaRequestHandlerServiceURI`  | Request Handler service identifier for locating files and drill down |
| `almaFileSodaServicePort`       | NGAS (Back-end) SODA service port        |
| `almaSODAServiceURI`            | User facing (Front-end) SODA service     |
| `almaDataPortalServiceURI`      | Data Portal service for DataLink entries |   
| `almaLoggingServiceURL`         | Full URL to the remote logging service to POST to |


### Docker Services

#### reg (ivo://almascience.org/reg) opencadc/alma-reg:2026q1

The Registry service to distribute URLs for running services, namely the SIAv2 and ObsCore services.

This service will have two files; `reg-applications.properties`, and `reg-resource-caps.properties`.

Once deployed, the ``reg`` service will make their contents available via the `/reg/applications` and `/reg/resource-caps` endpoints.

#### reg-applications.properties

Is expected to have two entries:

```properties
ivo://almascience.org/requesthandler=https://almasciencedl.eso.org/rh
ivo://almascience.org/dataportal=https://almascience.org/dataPortal
```

The URI keys will conform to the provided ones in the `org.opencadc.alma.properties`, and provide the services with an endpoint to
use it.  The `reg-applications.properties` differs in that it provides access to non-IVOA services, but still necessary ones.

#### reg-resource-caps.properties

Five IVOA services will need to be listed here, as well as the endpoints to use them.  These URI keys will also match the
configured ones in the `org.opencadc.alma.properties`.  The `data` service will be located by the Request Handler
location service due to the complexity of have the service run on multiple nodes.

```properties
# The registry service
ivo://almascience.org/reg = https://almascience.org/reg/capabilities

## ALMA services

# The IVAO SIA compatible service
ivo://almascience.org/sia = https://almascience.org/sia2/capabilities

# The IVOA DataLink service
ivo://almascience.org/datalink = https://almascience.org/datalink/capabilities

# The IVOA SODA service
ivo://almascience.org/soda = https://almascience.org/soda/capabilities

# The IVOA TAP service
ivo://almascience.org/tap = https://almascience.org/tap/capabilities
```

#### sia (ivo://almascience.org/sia) opencadc/alma-sia:2026q1

SIA v2 service.  This uses the ObsCore (TAP) service as described by the registry.  See the `SiaRunner.properties`
file to specify the TAP URI to use.

#### obscore (ivo://almascience.org/tap) opencadc/alma-tap:2026q1

ObsCore (TAP) service to query the ALMA ObsCore database.  An existing Oracle instance is required.

#### datalink (ivo://almascience.org/datalink) opencadc/alma-datalink:2026q1

DataLink service to expand an MOUS ID into download URLs

#### data (ivo://almascience.org/data) opencadc/alma-data:2026q1

Internal service to run on a storage (NGAS) node and execute the cutout code directly against files.  It is *mostly* SODA compliant.

#### soda (ivo://almascience.org/soda) opencadc/alma-soda:2026q1

IVOA SODA service.  This will use the `reg` service to locate the Request Handler service, and the back-end SODA service.

## Releases
The ALMA IVOA services are released on a quarterly (every three months) basis, namely to obtain the current underlying
image (Tomcat 9 with JDK 21) and ensure current CVE fixes.  These images are automatically built and
pushed to Docker Hub under the `opencadc` organization.