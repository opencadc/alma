-- Fails with ORA-00904 if view_target does not exist. Drop only after a successful upgrade ADD.
ALTER TABLE TAP_SCHEMA.tables11 DROP COLUMN view_target;
