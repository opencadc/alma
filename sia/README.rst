ALMA SIA (1.0.0)
================

`IVOA SIA`_ service for the `ALMA Science Archive`_.

Build
-----

Building the service creates a WAR artifact. From the root fo the
``alma-sia`` folder, run:

``gradle --info clean build``

to create the ``build/libs/sia##%VERSION%.war`` file, where the
``%VERSION%`` will be replaced with the actual version number declared
in the `build.gradle`_ file.

Alternatively, there is an ``alma_sia_app_name`` parameter that will replace the name of the output file.

``gradle -info -Palma_sia_app_name=my_alma_sia clean build``

will create the ``build/libs/my_alma_sia##%VERSION%.war`` file.  This can also be set in a local ``gradle.properties``,
or in you ``~/.gradle/gradle.properties`` file.

Deployment
----------

Docker
~~~~~~

This is a working prototype using an SIA implementation with an Oracle 11 *g* database.

After the `Build`_ step above, we can create a Docker deployment like so:

-  ``cp build/libs/*.war docker/``
-  ``cd docker/``
-  ``docker-compose up -d && ./waitForContainersReady.sh``

The necessary Docker images will be downloaded, including the large
Oracle one, then the service will be available on port ``8080``. You can
then issue a request like:

http://localhost:8080/sia2/availability

Which will provide you with an XML document as to the health of the
service. If it reads with the message:

``The SIA ObsCore service is accepting queries``

Then the SIA service is running properly. You can then issue a query to
the sample ObsCore table:

``curl -L -d 'QUERY=SELECT+TOP+1+*+FROM+TAP_SCHEMA.obscore&LANG=ADQL' http://localhost:8080/tap/sync``

Dedicated web server
~~~~~~~~~~~~~~~~~~~~

If you have a dedicated Servlet Container (i.e. `Tomcat`_) running
already, run the `Build`_ step above, then copy the WAR artifact from
``build/libs/`` to your Servlet Container’s webapp deployment directory.

.. _IVOA SIA: http://ivoa.net/Documents/SIA/
.. _ALMA Science Archive: http://almascience.nrao.edu/
.. _build.gradle: build.gradle
.. _Build: #build
.. _Tomcat: http://tomcat.apache.org
