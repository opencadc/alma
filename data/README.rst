ALMA Data service (0.2.0)
==============================

Simple data service to perform cutouts (sub-images) of files.  The current implementation only supports FITS files,
but can be extended in the future.

Endpoints
---------

Standard VOSI endpoints exist (``/capabilities``, and ``/availability``).

/data/files
~~~~~~~~~~

Access a FITS file.

GET/POST Query parameters:

+------------------+--------------------------------------+
| Name             | Purpose                              |
+==================+======================================+
| ``file``         | Absolute path to file                |
+------------------+--------------------------------------+
| ``{SODA-PARAM}`` | Pixel cutout spec.  Cannot be used   |
|                  | with ``headers`` parameter.          |
|                  |                                      |
|                  | ``...?SUB=[0][300:400]``             |
|                  | ``...?CIRCLE=123.0+89.4+0.02``       |
+------------------+--------------------------------------+
| ``headers``      | Set to ``true`` to see header list.  |
|                  | Cannot be used with ``{SODA-PARAM}`` |
|                  | parameter.                           |
+------------------+--------------------------------------+

Running Integration Tests
-------------------------

A local set of services will need to be started to run against with files located at:
https://www.canfar.net/storage/list/ALMA/test-data/cutouts

Add each file that does **not** have the ``-cutout`` name appendage.  The files with the ``-cutout`` appendage are used
to compare against the actual cutout that the test ran.

Using Docker Compose
~~~~~~~~~~~~~~~~~~~~

There is a ``docker-compose-ssl.yml`` file to bring up the necessary services with server certificates and client CA
certificates.  This requires more configuration as an implementor will need to have volumes created:

 - ``server_certs``: What server certificates for the proxy server (Traefik) to use to terminate SSL connections.
 - ``ca_certs``: Contains custom CA certificates to be included in the system's trusted CAs.  This is useful for development certificates.

There is also a ``docker-compose-nossl.yml`` that will just plain port 80 HTTP access.

``$ docker-compose -f docker-compose-[no]ssl.yml up -d``

