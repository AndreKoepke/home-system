CREATE TABLE IF NOT EXISTS config_close_contact
(
    name                   text primary key,
    message_when_trigger   text null,
    animation_when_trigger uuid null references animation (id)
);

