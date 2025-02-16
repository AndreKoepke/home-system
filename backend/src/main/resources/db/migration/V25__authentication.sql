CREATE TABLE config_auth_tokens
(
  id             uuid PRIMARY KEY,
  created        timestamp(6) NOT NULL,
  last_time_used timestamp(6) NOT NULL,
  description    varchar,
  token          varchar      NOT NULL
);
