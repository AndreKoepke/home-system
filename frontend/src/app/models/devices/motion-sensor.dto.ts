import {Device} from "./device.dto";

export interface MotionSensor extends Device {
  presence?: boolean;
  reachable: boolean;
  lastUpdate: string;
  presenceChangedAt: string;
  dark?: boolean;
  brightness?: boolean;
  config?: MotionSensorConfig;
}

export interface MotionSensorConfig {
  name: string;
  lights: string[];
  lightsAtNight: string[];
  keepMovingFor: string;
  onlyTurnOnWhenDarkerAs?: number;
  selfLightNoise?: number;
  turnLightOnWhenMovement: boolean;
  notBefore?: string;
  animationId?: string;
  animationAtNightId?: string;
}
