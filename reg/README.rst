ALMA Registry Service deployment
--------------------------------

This module packages the official OpenCADC registry image with ALMA-specific
configuration only. There is no Java build; the WAR and server come from
``images.opencadc.org/core/reg``.

Runtime configuration
~~~~~~~~~~~~~~~~~~~~~

The Docker image adds two files under ``/config`` (see ``docker/`` in this module):

- ``reg-resource-caps.properties`` — VOSI capabilities URLs for IVOA services
  (served at ``/reg/resource-caps``).
- ``reg-applications.properties`` — UI / application feature URLs
  (served at ``/reg/applications``).

Build the image from the ``reg`` directory::

  docker build -t opencadc/alma-reg:latest .

See the root ``README.md`` for the expected property entries and service URIs.