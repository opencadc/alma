-- Pick and choose as desired.  This file contains necessary items to create a Spatial Index column.

-- Patch the table, if needed
ALTER TABLE ALMA.asa_science
ADD SPATIAL_BOUNDS  SDO_GEOMETRY;

-- Populate the SPATIAL_BOUNDS column based on the current footprint values.  Takes minutes to run.
UPDATE ALMA.asa_science asr
SET asr.SPATIAL_BOUNDS = (SELECT TO_GEOMETRIC_OBJECT(asp.footprint) FROM ALMA.asa_science asp WHERE asp.DATASET_ID = asr.DATASET_ID);

-- Necessary to prepare Oracle for the Spatial Index coming.
INSERT INTO USER_SDO_GEOM_METADATA
VALUES ('ASA_SCIENCE','SPATIAL_BOUNDS', MDSYS.SDO_DIM_ARRAY(SDO_DIM_ELEMENT('RA', -360, 360, .0005), SDO_DIM_ELEMENT('DEC', -360, 360, .0005)), NULL);

-- Generate the Spatial Index.  Takes minutes to run.
CREATE INDEX ALMA.SPATIAL_BOUNDS_IDX ON ALMA.ASA_SCIENCE(SPATIAL_BOUNDS)
INDEXTYPE IS MDSYS.SPATIAL_INDEX
PARAMETERS ('layer_gtype=COLLECTION');

-- Trigger to maintain the column
CREATE OR REPLACE TRIGGER ALMA.SPATIAL_BOUNDS_TRIGGER
AFTER UPDATE ON ALMA.ASA_SCIENCE
	FOR EACH ROW
	BEGIN
		UPDATE ALMA.ASA_SCIENCE SET ALMA.ASA_SCIENCE.SPATIAL_BOUNDS = TO_GEOMETRIC_OBJECT( :NEW.FOOTPRINT ) WHERE ALMA.ASA_SCIENCE.DATASET_ID = :NEW.DATASET_ID;
	END;
