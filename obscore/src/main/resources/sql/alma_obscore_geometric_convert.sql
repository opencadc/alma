create or replace function TO_CIRCLE(in_coordinate_string in varchar2)
return SDO_GEOMETRY is
	circle SDO_GEOMETRY;
	buff		varchar2(2048);
	x_val 	number;
	y_val	number;
	radius_val	number;
	point_1_x number;
	point_1_y number;
	point_2_x number;
	point_2_y number;
	point_3_x number;
	point_3_y number;
	pattern varchar2(32) := '-?[0-9]\d*(\.\d+)?';
	begin
		if REGEXP_COUNT(in_coordinate_string, pattern) <> 3
		then
			RAISE_APPLICATION_ERROR(-20001, 'Circles require three (3) values: (x, y, radius)');
		end if;
		-- Set the X value as entered.
		select to_number(replace(regexp_substr(in_coordinate_string, pattern), '.', ',')) into x_val from DUAL;
		select trim(substr(in_coordinate_string, length(x_val) + 1)) into buff from DUAL;
		-- Set the Y value as entered.
		select to_number(replace(regexp_substr(buff, pattern), '.', ',')) into y_val from DUAL;
		select trim(substr(in_coordinate_string, length(y_val) + 1)) into buff from DUAL;
		-- Set the Radius value as entered.
		select to_number(replace(regexp_substr(buff, pattern), '.', ',')) into radius_val from DUAL;
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
	begin
		if REGEXP_COUNT(in_coordinate_string, pattern) < 6
		then
			RAISE_APPLICATION_ERROR(-20002, 'Polygons require three (3) points (six (6) values): (point1.x, point1.y, point2.x, point2.y, point3.x, point3.y)');
		end if;
		for i in c_vertices loop
      		counter := counter + 1;
      		next_vert := to_number(replace(i.VERT, '.', ','));
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
	union_geo SDO_GEOMETRY;
	next_shape SDO_GEOMETRY;
	begin
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
      		select SDO_GEOM.SDO_UNION(union_geo, next_shape, 0.05) into union_geo from DUAL;
      	end loop;
		return union_geo;
	end;
/


create or replace function TO_GEOMETRIC_OBJECT(in_footprint_icrs in varchar2)
return SDO_GEOMETRY is
	geo_shape SDO_GEOMETRY;
	parsed_footprint_between_paren varchar2(2048);
	footprint_type varchar2(8);
	parsed_footprint varchar2(2048);
	begin
		select replace(replace(regexp_substr(in_footprint_icrs, '\(.*\)'), '(', ''), ')', '') into parsed_footprint_between_paren from DUAL;
		select lower(regexp_substr(parsed_footprint_between_paren, '^\w+')) into footprint_type from DUAL;
		select trim(substr(parsed_footprint_between_paren, length(footprint_type) + 1)) into parsed_footprint from DUAL;
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
