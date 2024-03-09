DROP VIEW IF EXISTS config_motion_sensor_duration_view;

CREATE VIEW config_motion_sensor_duration_view AS
SELECT (keep_moving_for / 1000 || ' microseconds')::interval, *
FROM config_motion_sensor;
