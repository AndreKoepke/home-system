alter table config_cube
 add column if not exists device_name_on_side_1 text null,
 add column if not exists device_name_on_side_2 text null,
 add column if not exists device_name_on_side_3 text null,
 add column if not exists device_name_on_side_4 text null,
 add column if not exists device_name_on_side_5 text null,
 add column if not exists device_name_on_side_6 text null;
