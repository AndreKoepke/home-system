CREATE TABLE IF NOT EXISTS config_timer
(
  id          UUID PRIMARY KEY,
  name        VARCHAR NOT NULL,
  turn_on_at  TIME    NULL,
  turn_off_at TIME    NULL
);

CREATE TABLE IF NOT EXISTS config_timer_to_device
(
  timer_id    UUID    NOT NULL REFERENCES config_timer (id),
  device_name VARCHAR NOT NULL,
  CONSTRAINT pk_timer_to_device PRIMARY KEY (timer_id, device_name)
);
