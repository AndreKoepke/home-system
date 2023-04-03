create table public.animation
(
  id uuid not null
    primary key
);

create table public.animation_step_dimmer
(
  id            uuid not null
    primary key,
  dimm_duration numeric(21),
  dimm_light_to numeric(38, 2),
  name_of_light varchar(255),
  sort_order    integer,
  animation_id  uuid
    constraint fkglkeyic8fefd513fqot0alo6v
      references public.animation
);

create table public.animation_dimm_light_steps
(
  animation_id        uuid not null
    constraint fkpt61pmhw8pxhnj7v33q7qpeqp
      references public.animation,
  dimm_light_steps_id uuid not null
    constraint uk_q7ebovuh5c4gmjpxm2atm7v4w
      unique
    constraint fkn3q8rhwn3n41u8j16jut1tc4w
      references public.animation_step_dimmer
);

create table public.animation_step_on_off
(
  id            uuid not null
    primary key,
  name_of_light varchar(255),
  sort_order    integer,
  turn_it_on    boolean,
  animation_id  uuid
    constraint fkleut0djfrsv5h3n8me5of0k93
      references public.animation
);

create table public.animation_on_off_steps
(
  animation_id    uuid not null
    constraint fkad56ttrpkjmfxxkd442fg03hq
      references public.animation,
  on_off_steps_id uuid not null
    constraint uk_e6h458apg77t5v7ndb8tutlds
      unique
    constraint fke17cwuqytthqn41j547p91ifw
      references public.animation_step_on_off
);

create table public.animation_step_pause
(
  id           uuid not null
    primary key,
  sort_order   integer,
  wait_for     numeric(21),
  animation_id uuid not null
    constraint fk_pause_step_animation references public.animation (id)
);

create table public.animation_pause_steps
(
  animation_id   uuid not null
    constraint fkt6t8iwudantr5ru63ataa2jpk
      references public.animation,
  pause_steps_id uuid not null
    constraint uk_sw0r3yvi5tp3cwv8c97dn1va9
      unique
    constraint fkip27455y1t06ow2krpor6byai
      references public.animation_step_pause
);

create table public.config_basic
(
  modified                          timestamp(6) not null
    primary key,
  good_night_button_event           integer,
  good_night_button_name            text,
  latitude                          double precision,
  longitude                         double precision,
  main_door_name                    varchar(255),
  nearest_weather_cloud_station     text,
  night_run_scene_name              text,
  night_scene_name                  text,
  send_message_when_turn_lights_off boolean      not null,
  sunset_scene_name                 text,
  when_main_door_opened_id          uuid
    constraint fkejym5ujfhn97ug8nc3str8heb
      references public.animation
);

create table public.config_basic_not_lights
(
  basic_config_modified timestamp(6) not null
    constraint fkflf96jvakdqtpv5we8blyvc0v
      references public.config_basic,
  not_lights            text
);

create table public.config_deconz
(
  modified       timestamp(6) not null
    primary key,
  api_key        text,
  host           text,
  port           integer      not null,
  websocket_port integer      not null
);

create table public.config_fan
(
  name                               varchar(255) not null
    primary key,
  increase_timeout_for_motion_sensor text,
  trigger_by_button_event            integer,
  trigger_by_button_name             text,
  turn_off_when_light_turned_off     text
);

create table public.config_mastodon
(
  modified timestamp(6) not null
    primary key,
  server   varchar(255),
  token    varchar(255)
);

create table public.config_motion_sensor
(
  name            text not null
    primary key,
  keep_moving_for numeric(21)
);

create table public.config_motion_sensor_lights
(
  motion_sensor_config_name text not null
    constraint fka9jd6w0s6ok6le5taucjkk60x
      references public.config_motion_sensor,
  lights                    text
);

create table public.config_off_button
(
  name         text not null
    primary key,
  button_event integer
);

create table public.config_openai
(
  modified timestamp(6) not null
    primary key,
  api_key  varchar(255),
  size     smallint
);

create table public.config_power_meter
(
  name                    text not null
    primary key,
  is_on_when_more_than    integer,
  message_when_switch_off text,
  message_when_switch_on  text,
  linked_fan_name         varchar(255)
    constraint fk6ns054hdooc3xxy0aep8rmyyy
      references public.config_fan
);

create table public.config_roller_shutter
(
  name                          text    not null
    primary key,
  close_at                      time,
  compass_direction             text,
  ignore_weather_in_the_evening boolean not null,
  ignore_weather_in_the_morning boolean not null,
  open_at                       time
);

create table public.config_telegram
(
  modified     timestamp(6) not null
    primary key,
  bot_path     text,
  bot_token    text,
  bot_username text,
  main_channel text
);

create table public.config_user
(
  name        text    not null
    primary key,
  dev         boolean not null,
  device_ip   text,
  telegram_id text
);

create table if not exists public.openai_images
(
  created    timestamp(6) not null
    primary key,
  downloaded integer      not null,
  image      bytea,
  prompt     varchar(255) not null
);

create table if not exists public.rain_stats
(
  measured_at timestamp(6) not null
    primary key,
  raining     boolean      not null
);

create table if not exists public.config_cube
(
  name                 text primary key,
  scene_name_on_side_1 text null,
  scene_name_on_side_2 text null,
  scene_name_on_side_3 text null,
  scene_name_on_side_4 text null,
  scene_name_on_side_5 text null,
  scene_name_on_side_6 text null,
  scene_name_on_shake  text null
);

