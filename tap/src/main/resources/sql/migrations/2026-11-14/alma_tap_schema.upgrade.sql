-- ALMA TAP schema patch (existing Oracle deployments).
-- Safe to re-run: column datatype/arraysize updates are idempotent;
-- view_target ADD will fail if the column already exists (skip that block if so).

-- 1) TAP_SCHEMA.tables11: view_target (cadc-tap-schema 1.2.10)
ALTER TABLE TAP_SCHEMA.tables11 ADD (view_target VARCHAR2(128));

UPDATE TAP_SCHEMA.tables11
   SET view_target = 'alma.obscore'
 WHERE table_name = 'ivoa.obscore';

UPDATE TAP_SCHEMA.tables11
   SET view_target = 'alma.source_cone_search'
 WHERE table_name = 'sourcecatalogue.source_cone_search';

-- 2) columns11: TAP 1.0 adql:BIGINT → VOTable datatype long
UPDATE TAP_SCHEMA.columns11
   SET datatype = 'long'
 WHERE xtype = 'adql:BIGINT';

-- 3) columns11: DALI UTC timestamp arraysize (10, 19, or 23; use 23* = IVOA ms precision)
UPDATE TAP_SCHEMA.columns11
   SET arraysize = '23*',
       "size" = 23
 WHERE xtype IN ('timestamp', 'adql:TIMESTAMP');
