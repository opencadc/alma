ALMA TAP SQL
============

SQL files to initialize the ALMA Oracle database.

- Tested with Oracle 11 and 12
 

.. table:: Files in order
  :widths: auto
  
====================================== ======================================
File                                   Purpose                              
====================================== ======================================
``alma_obscore_geometric_convert.sql`` Create functions to convert Footprint values into Oracle Spatial Geometric objects.
``alma_obscore_view.sql``              Create the ObsCore View.
``alma_tap_schema_content.sql``        Create the TAP content to query the ObsCore View.
``alma_obscore_spatial_index.sql``     Create the Oracle spatial index on the geometric columns in the science table.  This should be run *after* the science table's geometric objects have been created.
====================================== ======================================

This folder also contains versioned files of the above.  They are specific to ALMA's deployment and should be used with caution.
