==========
Change Log
==========

1.1.10 (Released)
-------------------------

* Added formatter for region (S_REGION) column.
* ADQL properly refers to the FOOTPRINT column when selecting the S_REGION column.
* Added formatting to the TO_NUMBER() call in the SQL functions.  Some Oracle instances use a comma (,) and others use a period (.).
* Improved function to create the UNION types in S_REGION.

1.1.9 (Released)
-------------------------

* Remove unused line in web.xml

1.1.8 (Released)
-------------------------

* Completed SDO_GEOMETRY column Footprint conversions for Polygons, Circles, and Unions.

1.1.7 (Released)
-------------------------

* Convert ``DATE`` types to ``TIMESTAMP`` types for IVOA compatibility.


1.1.6 (Released)
-------------------------

* Fixed all docker-compose files for volume mounts.
* Fixed Travis builds with JDK 11.
* Added Gradle Wrapper (./gradlew).
* Added Swagger documentation

1.1.5 (Released)
-------------------------

* Fix YOU SHOULD NOT SEE THIS MESSAGE in logs.
* Add TO_UNION function and modify TO_GEOMETRIC_OBJECT to use it.
* Add SSL options in Docker deployment.

1.1.4 (Defunct)
-------------------------

1.1.3 (Released)
-------------------------

* Change property names for JDBC pool to match CADC standards.
* Moved docker deployment items into docker folders.

1.1.2 (Released)
-------------------------

* Add support for the ``DISTANCE`` function.
* Fix for ``TOP`` keyword being executed before aggregate functions.
