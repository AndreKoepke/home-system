ALTER TABLE config_lightness_controlled_device
  ADD IF NOT EXISTS keep_off_from time WITHOUT TIME ZONE NULL,
  ADD IF NOT EXISTS keep_off_to   TIME WITHOUT TIME ZONE;
