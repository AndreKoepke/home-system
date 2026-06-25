ALTER TABLE config_roller_shutter
  DROP CONSTRAINT IF EXISTS config_roller_shutter_pkey;
ALTER TABLE config_roller_shutter
  ADD PRIMARY KEY (name);

ALTER TABLE config_motion_sensor
  ADD COLUMN IF NOT EXISTS turn_on_when_roller_shutter_is_closed varchar REFERENCES config_roller_shutter (name);
