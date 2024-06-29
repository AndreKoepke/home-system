CREATE TABLE IF NOT EXISTS config_timed_button
(
  button_name  VARCHAR NOT NULL PRIMARY KEY,
  button_event INTEGER NOT NULL,
  keep_on_for  BIGINT  NOT NULL,
  lights      TEXT    NOT NULL
);
