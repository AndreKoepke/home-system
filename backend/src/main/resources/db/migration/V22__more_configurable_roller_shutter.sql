ALTER TABLE config_roller_shutter
  ADD COLUMN high_sun_level        int DEFAULT 300,
  ADD COLUMN close_level_low_tilt  int DEFAULT 40,
  ADD COLUMN close_level_high_tilt int DEFAULT 75,
  ADD COLUMN close_level_low_lift  int DEFAULT 50,
  ADD COLUMN close_level_high_lift int DEFAULT 75;
