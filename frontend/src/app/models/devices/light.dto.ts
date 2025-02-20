export interface Light {
  id: string;
  name: string;
  brightness: number;
  on: boolean;
  reachable: boolean;
  lastUpdated: string;
}
