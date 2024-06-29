ALTER TABLE config_roller_shutter
  ADD COLUMN IF NOT EXISTS close_with_interrupt BOOLEAN NULL;
