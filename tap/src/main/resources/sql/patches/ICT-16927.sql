INSERT INTO TAP_SCHEMA.columns11
(table_name, column_name, utype, ucd, unit, description, datatype, arraysize, xtype, "size", principal, indexed, std, column_index, id)
VALUES
('ivoa.obscore', 'collections', null, null, null, 'Indicates that there are external products', 'char', '*', 'adql:VARCHAR', 128, 1, 0, 1, (select max(column_index) + 1 from TAP_SCHEMA.columns11), null)