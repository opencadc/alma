--
-- Upgrade from TAP 1.1.6 to 1.2.0
-- Add permission columns to support user-created content, if required.
--
-- jenkinsd 2019.09.23
--


ALTER TABLE TAP_SCHEMA.schemas11
    ADD (owner_id         VARCHAR2(32),
         read_anon        NUMBER,
         read_only_group  VARCHAR2(128),
         read_write_group VARCHAR2(128));

ALTER TABLE TAP_SCHEMA.tables11
    ADD (owner_id         VARCHAR2(32),
         read_anon        NUMBER,
         read_only_group  VARCHAR2(128),
         read_write_group VARCHAR2(128));