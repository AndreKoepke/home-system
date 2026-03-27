import {Device} from "./device.dto";
import {AnimationDto} from "../animation.dto";

export interface MotionSensor extends Device {
  presence?: boolean;
  reachable: boolean;
  lastUpdate: string;
  presenceChangedAt: string;
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
  notBefore: string;
  animation?: AnimationDto;
  animationAtNight?: AnimationDto;
}
