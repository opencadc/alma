ALMA IVOA Deployment
====================

ALMA Science Archive web services.


Docker Compose deployment
-------------------------

For ease, rename one of these to just ``docker-compose.yml``, or run with ``docker-compose -f <file>``.

docker-compose-nossl.yml
~~~~~~~~~~~~~~~~~~~~~~~~

A set of HTTP-only running services relying on an existing Oracle database.  Port 80 is open.

docker-compose-ssl.yml
~~~~~~~~~~~~~~~~~~~~~~

A set of HTTPS-only running services relying on an existing Oracle database.  This requires a
volume called ``server_certs`` containing ``server.crt`` and ``server.key`` to enable Traefik's SSL engine.  Ports 80 and 443 are open.

For any Docker Compose deployment, provide a ``.env`` file with the following variables:

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

.. code-block::

  PROXY_HOST=myhost.example.com
  DATABASE_USER=myuser
  DATABASE_PASSWORD=myuserpw
  DATABASE_HOST=db.host.example.com
  DATABASE_NAME=ALMA


Service Stack Configuration
---------------------------

The properties file, ``org.opencadc.alma.properties``, is required to be in the ``~/config/`` folder.  This file contains the Service URIs (identifiers)
that will be looked up in the Registry to obtain an acutal URL.


**org.opencadc.alma.properties**

.. code-block::

  almaDataLinkServiceURI=ivo://almascience.org/datalink
  almaRequestHandlerServiceURI=ivo://almascience.org/requesthandler
  almaFileSodaServiceURI=ivo://almascience.org/data
  almaSODAServiceURI=ivo://almascience.org/soda
  almaDataPortalServiceURI=ivo://almascience.org/dataportal


  +-------------------------------+------------------------------------------+
  | Property                      | Description                              |
  +===============================+==========================================+
  | almaDataLinkServiceURI        | DataLink service identifier              |
  +-------------------------------+------------------------------------------+
  | almaRequestHandlerServiceURI  | Request Handler service identifier       |
  |                               | for locating files and drill down        |
  +-------------------------------+------------------------------------------+
  | almaFileSodaServiceURI        | NGAS (Back-end) SODA service             |
  +-------------------------------+------------------------------------------+
  | almaSODAServiceURI            | User facing (Front-end) SODA service     |
  +-------------------------------+------------------------------------------+
  | almaDataPortalServiceURI      | Data Portal service for DataLink entries |   
  +-------------------------------+------------------------------------------+


Services
~~~~~~~~

reg (ivo://almascience.org/reg) opencadc/alma-reg:1.0.2
+++++++++++++++++++++++++++++++++++++++++++++++++++++++

The Registry service to distribute URLs for running services, namely the SIA and ObsCore services.

This service will have two files; ``reg-applications.properties``, and ``reg-resource-caps.properties``.

Once deployed, the ``reg`` service will make their contents available via the ``/reg/applications`` and ``/reg/resource-caps`` endpoints.

**reg-applications.properties**

Is expected to have two entries:

.. code-block::

    ivo://almascience.org/requesthandler=https://almasciencedl.eso.org/rh
    ivo://almascience.org/dataportal=https://almascience.org/dataPortal

The URI keys will conform to the provided ones in the ``org.opencadc.alma.properties``, and provide the services with an endpoint to
use it.  The ``reg-applications.properties`` differs in that it provides access to non-IVOA services, but still necessary ones.

**reg-resource-caps.properties**

All six IVOA services will need to be listed here, as well as the endpoints to use them.  These URI keys will also match the
configured ones in the ``org.opencadc.alma.properties``.

.. code-block::

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

    # The ALMA Data service
    ivo://almascience.org/data = https://almascience.org/data/capabilities



sia (ivo://almascience.org/sia) opencadc/alma-sia:1.0.4
+++++++++++++++++++++++++++++++++++++++++++++++++++++++

SIA v2 service.  This uses the ObsCore (TAP) service as described by the registry.  See the ``SiaRunner.properties``
file to specify the TAP URI to use.

obscore (ivo://almascience.org/tap) opencadc/alma-tap:1.1.16
++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++

ObsCore (TAP) service to query the ALMA ObsCore database.  An existing Oracle instance is required.

datalink (ivo://almascience.org/datalink) opencadc/alma-datalink:1.3.4
++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++

DataLink service to expand an MOUS ID into download URLs

data (ivo://almascience.org/data) opencadc/alma-data:1.0.0
++++++++++++++++++++++++++++++++++++++++++++++++++++++++++

Internal service to run on a storage (NGAS) node and execute the cutout code directly against files.  It is *mostly* SODA compliant.

soda (ivo://almascience.org/soda) opencadc/alma-soda:1.2.0
++++++++++++++++++++++++++++++++++++++++++++++++++++++++++

IVOA SODA service.  This will use the ``reg`` service to locate the Request Handler service, and the back-end SODA service.
