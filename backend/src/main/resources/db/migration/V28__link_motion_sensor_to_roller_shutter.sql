ALTER TABLE config_motion_sensor
  ADD COLUMN IF NOT EXISTS turn_on_when_roller_shutter_is_closed varchar REFERENCES config_roller_shutter (name);
