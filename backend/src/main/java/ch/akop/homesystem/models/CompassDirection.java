package ch.akop.homesystem.models;

import lombok.Getter;
import lombok.RequiredArgsConstructor;


@Getter
@RequiredArgsConstructor
public enum CompassDirection {

  NORTH(0),
  NORTH_EAST(45),
  EAST(90),
  SOUTH_EAST(135),
  SOUTH(180),
  SOUTH_WEST(225),
  WEST(270),
  NORTH_WEST(315);
  private final int direction;
}
