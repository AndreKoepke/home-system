export interface Weather {
  recordedAt: string;
  outerTemperature: ValueAndUnit;
  wind: ValueAndUnit;
  outerLight: ValueAndUnit;
  rain: ValueAndUnit
}

export interface ValueAndUnit {
  value: number;
  unit: string;
}

