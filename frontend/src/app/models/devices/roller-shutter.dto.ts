import {Device} from "./device.dto";

export interface RollerShutter extends Device {

  currentLift: number;
  currentTilt: number;
  open: boolean;
  config?: {
    compassDirection: string[];
    closeAt: string;
    openAt: string;
    noAutomaticsUntil: string | undefined;
    closeWithInterrupt: string;
  }
}
