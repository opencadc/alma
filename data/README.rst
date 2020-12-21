ALMA Data service (1.0.0)
==============================

Simple data service to perform cutouts (sub-images) of files.  The current implementation only supports FITS files,
but can be extended in the future.

Endpoints
---------

/data/fits
~~~~~~~~~~

Access a FITS file.

GET/POST Query parameters:

+------------+--------------------------------------+
| Name       | Purpose                              |
+============+======================================+
| ``file``   | Absolute path to file                |
+------------+--------------------------------------+
| ``cutout`` | Pixel cutout spec.  Cannot be used   |
|            | with ``headers`` parameter.          |
|            |                                      |
|            | ``...?cutout=[0][300:400]``          |
+------------+--------------------------------------+
| ``headers``| Set to true to see header listing.   |
|            | Cannot be used with ``cutout``       |
|            | parameter.                           | 
+------------+--------------------------------------+

