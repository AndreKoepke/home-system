export interface Weather {

  recordedAt: Date;
  outerTemperatur: {
    valueAsBaseUnit: number;
    baseUnit: "DEGREE" | "FAHRENHEIT" | "KELVIN";
  }

  wind: {
    valueAsBaseUnit: number;
    baseUnit: "METERS_PER_SECOND" | "KILOMETERS_PER_SECOND";
  }

  light: {
    valueAsBaseUnit: number;
    baseUnit: "KILO_LUX"
  }

  rain: {
    valueAsBaseUnit: number;
    baseUnit: "MILLIMETER_PER_HOUR"
  }

}

