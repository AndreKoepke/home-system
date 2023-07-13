ALTER TABLE config_power_meter
  ADD COLUMN turn_off_when_ready            boolean DEFAULT false,
  ADD COLUMN got_ready_when_nobody_was_home boolean DEFAULT false,
  ADD COLUMN sent_reminders_count           int     DEFAULT 0,
  ADD COLUMN pauses_during_run              int NULL;
