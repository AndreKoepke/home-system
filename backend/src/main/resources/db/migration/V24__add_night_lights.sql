ALTER TABLE config_motion_sensor
  ADD COLUMN IF NOT EXISTS animation_id_night uuid NULL REFERENCES animation (id),
  ADD PRIMARY KEY (name);

CREATE TABLE IF NOT EXISTS config_motion_sensor_lights_night
(
  motion_sensor_config_name text NOT NULL REFERENCES config_motion_sensor UNIQUE,
  lights                    text
);
