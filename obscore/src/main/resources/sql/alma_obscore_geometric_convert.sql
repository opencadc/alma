create or replace function TO_CIRCLE(in_coordinate_string in varchar2)
return SDO_GEOMETRY is
	circle SDO_GEOMETRY;
    begin_index number;
    x_val number;
    y_val number;
    radius_val number;
    point_1_x number;
    point_1_y number;
    point_2_x number;
    point_2_y number;
    point_3_x number;
    point_3_y number;
    pattern varchar2(32) := '-?[0-9]\d*(\.\d+)?';
    format varchar2(16) := '999.999999';
begin
		if REGEXP_COUNT(in_coordinate_string, pattern) <> 3
		then
			RAISE_APPLICATION_ERROR(-20001, 'Circles require three (3) values: (x, y, radius)');
        end if;
        select regexp_instr(in_coordinate_string, pattern) into begin_index from DUAL;
        select to_number(regexp_substr(in_coordinate_string, pattern, begin_index, 1), format, ' NLS_NUMERIC_CHARACTERS = '',.''') into x_val from DUAL;
        select to_number(regexp_substr(in_coordinate_string, pattern, begin_index, 2), format, ' NLS_NUMERIC_CHARACTERS = '',.''') into y_val from DUAL;
        select to_number(regexp_substr(in_coordinate_string, pattern, begin_index, 3), format, ' NLS_NUMERIC_CHARACTERS = '',.''') into radius_val from DUAL;
        select x_val - radius_val into point_1_x from DUAL;
        select y_val into point_1_y from DUAL;
        select x_val into point_2_x from DUAL;
        select y_val + radius_val into point_2_y from DUAL;
        select x_val + radius_val into point_3_x from DUAL;
        select y_val into point_3_y from DUAL;
        select SDO_GEOMETRY(2003, null, null, SDO_ELEM_INFO_ARRAY(1, 1003, 4), SDO_ORDINATE_ARRAY(point_1_x, point_1_y, point_2_x, point_2_y, point_3_x, point_3_y)) into circle from DUAL;
        return circle;
end;
/

create or replace function TO_POLYGON(in_coordinate_string in varchar2)
return SDO_GEOMETRY is
	pattern varchar2(32) := '-?[0-9]\d*(\.\d+)?';
	cursor c_vertices is
	select regexp_substr(in_coordinate_string, pattern, 1, level) as VERT from DUAL
	connect by regexp_substr(in_coordinate_string, pattern, 1, level) is not null;
	poly SDO_GEOMETRY;
	vertices SDO_ORDINATE_ARRAY := SDO_ORDINATE_ARRAY();
	counter integer := 0;
	next_vert number := 0.0;
    format varchar2(16) := '999.99999999';
	begin
		if REGEXP_COUNT(in_coordinate_string, pattern) < 6
		then
			RAISE_APPLICATION_ERROR(-20002, 'Polygons require three (3) points (six (6) values): (point1.x, point1.y, point2.x, point2.y, point3.x, point3.y)');
		end if;
		for i in c_vertices loop
      		counter := counter + 1;
      		next_vert := to_number(i.VERT, format, ' NLS_NUMERIC_CHARACTERS = '',.''');
      		vertices.extend;
      		vertices(counter) := next_vert;
      	end loop;
		select SDO_GEOMETRY(2003, null, null, SDO_ELEM_INFO_ARRAY(1, 1003, 1), vertices) into poly from DUAL;
		return poly;
	end;
/

create or replace function TO_UNION(in_coordinate_string in varchar2)
return SDO_GEOMETRY is
    pattern varchar2(64) := '((Polygon|Circle)(\ -?[0-9]\d*(\.\d+))*)+';
    cursor c_shapes is
    select regexp_substr(in_coordinate_string, pattern, 1, level) as SHAPE from DUAL
    connect by regexp_substr(in_coordinate_string, pattern, 1, level) is not null;
    shape_array SDO_GEOMETRY_ARRAY;
    next_shape SDO_GEOMETRY;
    shape_collector SDO_GEOMETRY_ARRAY;
    begin
    shape_collector := SDO_GEOMETRY_ARRAY();
    for i in c_shapes loop
        if LOWER(i.SHAPE) LIKE 'circle%'
        then
            next_shape := TO_CIRCLE(i.SHAPE);
        elsif LOWER(i.SHAPE) LIKE 'polygon%'
        then
            next_shape := TO_POLYGON(i.SHAPE);
        else
            RAISE_APPLICATION_ERROR(-20000, 'Unsupported shape in UNION"' || i.SHAPE || '".  Only "CIRCLE" or "POLYGON" are supported.');
        end if;
        shape_collector.extend;
        shape_collector(shape_collector.count) := next_shape;
    end loop;
    return SDO_AGGR_SET_UNION(shape_collector, 0.000001);
end;
/


create or replace function TO_GEOMETRIC_OBJECT(in_footprint_icrs in varchar2)
return SDO_GEOMETRY is
	geo_shape SDO_GEOMETRY;
	footprint_type varchar2(8);
	parsed_footprint varchar2(4096);
	begin
		select lower(regexp_substr(in_footprint_icrs, '^\w+')) into footprint_type from DUAL;
		select trim(substr(in_footprint_icrs, length(footprint_type) + 1)) into parsed_footprint from DUAL;
		if footprint_type = 'circle'
		then
			geo_shape := TO_CIRCLE(parsed_footprint);
		elsif footprint_type = 'polygon'
		then
			geo_shape := TO_POLYGON(parsed_footprint);
		elsif footprint_type = 'union'
		then
		     geo_shape := TO_UNION(parsed_footprint);
		elsif footprint_type = '' or footprint_type is null
		then
			geo_shape := null;
		else
		 	RAISE_APPLICATION_ERROR(-20000, 'Unsupported shape "' || footprint_type || '".  Only "CIRCLE", "POLYGON", or "UNION" are supported.');
		end if;
		return geo_shape;
     end;
/
