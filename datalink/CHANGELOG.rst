ALMA DataLink Service (1.4.0)
=============================

2022.02.21 - 1.4.0 - opencadc/alma-datalink:1.4.0
-------------------------------------------------

- Use latest DataLink vocabulary (https://www.ivoa.net/rdf/datalink/core)
- Populate `description` field
- Restrict semantics to a single value

2020.05.26 - 1.1.0 (Released) - opencadc/alma-datalink:1.1.0
------------------------------------------------------------

- Remove dependency on all ALMA APIs
- Code cleanup
- Remove all configuration for databases and ALMA APIs


2019.07.12 - 1.0.0 (Released) - opencadc/alma-datalink:1.0.0
------------------------------------------------------------

- Add DataLink output URLs.
    - MOUS IDs will generate URLs for packages and README files
    - Package file IDs will generate a list of individual files (FITS, GZ, Scripts, Calibration, etc.)
- Add output for Cutout URLs where appropriate (FITS files only for now)
- Add Dockerized deployment
    - Including SSL if desired.
