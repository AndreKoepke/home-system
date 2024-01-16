CREATE TABLE IF NOT EXISTS config_timed_button
(
  buttonName  VARCHAR NOT NULL PRIMARY KEY,
  buttonEvent INTEGER NOT NULL,
  keepOnFor   BIGINT  NOT NULL,
  lights      TEXT    NOT NULL
);
