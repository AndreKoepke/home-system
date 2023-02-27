ALTER TABLE animation
    ADD PRIMARY KEY (id);

ALTER TABLE config_motion_sensor
    ADD COLUMN only_at_normal_state        bool NULL,
    ADD COLUMN only_turn_on_when_darker_as int  NULL,
    ADD COLUMN animation_id                uuid NULL REFERENCES animation (id);

