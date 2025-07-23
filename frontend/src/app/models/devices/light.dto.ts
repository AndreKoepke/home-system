import {Device} from "./device.dto";

export interface Light extends Device {
  brightness: number;
  on: boolean;
  reachable: boolean;
  lastUpdated: string;
}
