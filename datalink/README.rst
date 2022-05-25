ALMA DataLink (1.4.0)
================

Current implementation can be found at the CADC here:
https://beta.cadc-ccda.hia-iha.nrc-cnrc.gc.ca/alma-datalink/


`IVOA DataLink`_ service for the `ALMA Science Archive`_.

Build
-----

Building the service creates a WAR artifact. From the root fo the
``alma.git/datalink`` folder, run:

``gradle --info clean build test``

to create the ``build/libs/datalink.war`` file.

Deployment
----------

Docker
~~~~~~

This is a working prototype using a DataLink implementation.  An existing image can be found here:

``docker pull opencadc/alma-datalink:1.4.0``

Building for Docker
~~~~~~~~~~~~~~~~~~~

After the `Build`_ step above, we can create a Docker deployment like
so:

-  ``docker build -t alma-datalink-image:1.0 .``

The necessary Docker images will be downloaded, including the large
Oracle one, then the service will be available on port ``8080``. You can
then issue a request like:

http://localhost:8080/datalink/availability

Which will provide you with an XML document as to the health of the
service. If it reads with the message:

``DataLink state: ACTIVE``

Then the DataLinkj service is running properly. You can then request the URLs for data for an MOUS ID:

``curl -L http://localhost:8080/datalink/sync?ID=uid://A001/X87c/X3f1``

Dedicated web server
~~~~~~~~~~~~~~~~~~~~

If you have a dedicated Servlet Container (i.e. `Tomcat`_) running
already, run the `Build`_ step above, then copy the WAR artifact from
``build/libs/`` to your Servlet Container’s webapp deployment directory.

Integration Testing
-------------------

Integration tests are provided to test the deployed system.  From the root run:

``gradle -i clean intTest``

Set the Registry to a custom host, which will look for the IVOA registry capabilities document at myhost.nrao.edu/reg/resource-caps:

``gradle -i -Dca.nrc.cadc.reg.client.RegistryClient.host=myhost.nrao.edu clean intTest``

Or set to a completely custom location:

``DATALINK_REGISTRY_URL=https://myreghost.nrao.edu/alt-reg/capabilities.out gradle -i clean intTest``


.. _IVOA DataLink: http://www.ivoa.net/documents/DataLink/
.. _ALMA Science Archive: http://almascience.nrao.edu/
.. _build.gradle: build.gradle
.. _Build: #build
.. _WAR File: datalink
.. _Tomcat: http://tomcat.apache.org
