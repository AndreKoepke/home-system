ALTER TABLE config_power_meter
  ADD COLUMN turn_off_when_ready            bool DEFAULT false NOT NULL,
  ADD COLUMN got_ready_when_nobody_was_home bool DEFAULT false NOT NULL,
  ADD COLUMN sent_reminders_count           int  DEFAULT 0     NOT NULL;
