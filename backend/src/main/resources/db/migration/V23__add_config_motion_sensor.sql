ALTER TABLE config_motion_sensor
  ADD COLUMN IF NOT EXISTS self_light_noise int NULL;
