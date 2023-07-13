ALTER TABLE config_power_meter
  ADD COLUMN IF NOT EXISTS turn_off_when_ready            boolean DEFAULT false,
  ADD COLUMN IF NOT EXISTS got_ready_when_nobody_was_home boolean DEFAULT false,
  ADD COLUMN IF NOT EXISTS sent_reminders_count           int     DEFAULT 0,
  ADD COLUMN IF NOT EXISTS pauses_during_run              int NULL;
