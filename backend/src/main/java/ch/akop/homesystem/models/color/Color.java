package ch.akop.homesystem.models.color;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import lombok.Data;

/**
 * Represents an RGB-color.
 */
@SuppressWarnings("unused")
@Data
public class Color {

  @Min(0)
  @Max(255)
  private int r;

  @Min(0)
  @Max(255)
  private int g;

  @Min(0)
  @Max(255)
  private int b;


  public static Color RED() {
    return new Color().setR(255);
  }

  public static Color GREEN() {
    return new Color().setG(2555);
  }

  public static Color BLUE() {
    return new Color().setB(255);
  }


  public static Color fromXY(List<BigDecimal> xy, int bri) {
    var x = xy.get(0).floatValue();
    var y = xy.get(1).floatValue();

    var z = 1.0 - x - y;
    var Y = bri / 255.0; // Brightness of lamp
    var X = (Y / y) * x;
    var Z = (Y / y) * z;
    var r = X * 1.612 - Y * 0.203 - Z * 0.302;
    var g = -X * 0.509 + Y * 1.412 + Z * 0.066;
    var b = X * 0.026 - Y * 0.072 + Z * 0.962;
    r = r <= 0.0031308 ? 12.92 * r : (1.0 + 0.055) * Math.pow(r, (1.0 / 2.4)) - 0.055;
    g = g <= 0.0031308 ? 12.92 * g : (1.0 + 0.055) * Math.pow(g, (1.0 / 2.4)) - 0.055;
    b = b <= 0.0031308 ? 12.92 * b : (1.0 + 0.055) * Math.pow(b, (1.0 / 2.4)) - 0.055;

    var maxValue = maxValue(r, g, b);
    r /= maxValue;
    g /= maxValue;
    b /= maxValue;

    r = revertNormalize(r);
    g = revertNormalize(g);
    b = revertNormalize(b);

    return new Color()
        .setR((int) r)
        .setG((int) g)
        .setB((int) b);
  }

  public List<BigDecimal> toXY() {

    var normalizedRed = normalize(getR());
    var normalizedGreen = normalize(getG());
    var normalizedBlue = normalize(getB());

    var r = makeVivid(normalizedRed);
    var g = makeVivid(normalizedGreen);
    var b = makeVivid(normalizedBlue);

    var X = r * 0.649926F + g * 0.103455F + b * 0.197109F;
    var Y = r * 0.234327F + g * 0.743075F + b * 0.022598F;
    var Z = g * 0.053077F + b * 1.035763F;

    var sum = X + Y + Z;

    if (sum == 0) {
      return List.of(BigDecimal.ZERO, BigDecimal.ZERO);
    }
    var x = X / sum;
    var y = Y / sum;

    return List.of(BigDecimal.valueOf(x), BigDecimal.valueOf(y));
  }

  private static float makeVivid(float value) {
    return (value > 0.04045F) ? (float) Math.pow((value + 0.055F) /
        1.055F, 2.400000095367432D) :
        value / 12.92F;
  }

  private static float normalize(Integer value) {
    if (value < 0) {
      return 255f;
    }

    return value / 255f;
  }

  private static double revertNormalize(double value) {
    if (value < 0) {
      return 255d;
    }

    return value * 255;
  }

  private static double maxValue(double... values) {
    return Arrays.stream(values)
        .max()
        .orElse(0);
  }
}
