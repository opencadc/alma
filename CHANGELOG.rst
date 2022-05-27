ALMA IVOA Service Stack
=======================

2022.02.21
----------

- DataLink service now restricts to a single semantic
- DataLink uses the latest vocabulary

2020.01.27
----------

- Data service now streams output to avoid memory consumption issues.
- Data service now complies with experimental SODA APIs.

2020.12.21
----------

- Added data service to access underlying files.

2019.07.29
----------

- Added integration tests for SODA.
- Added README instructions.

2019.07.26
----------

- First pass at SODA service
- Added a single properties file at the root (``org.opencadc.alma.properties``)
- Updated docker-compose files
- Added ``alma-lib`` shared library for common API between DataLink and SODA
