import {Device} from "./device.dto";

export interface MotionSensor extends Device {
  presence?: boolean;
  reachable: boolean;
  lastUpdate: string;
  presenceChangedAt: string;
}
