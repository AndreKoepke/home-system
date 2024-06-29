CREATE TABLE config_lightness_controlled_device
(
  name                     text NOT NULL PRIMARY KEY,
  turn_on_when_darker_as   INT  NULL,
  turn_off_when_lighter_as INT  NULL
);
