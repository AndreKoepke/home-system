export interface Sensor {
  id: string;
  name: string;
  presence?: boolean;
  reachable: boolean;
  lastUpdated: string;
}
