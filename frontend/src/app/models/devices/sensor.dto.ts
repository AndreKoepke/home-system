export interface Sensor {
  id: string;
  name: string;
  presence?: boolean;
  reachable: boolean;
  lastUpdate: string;
  presenceChangedAt: string;
}
