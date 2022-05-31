package ch.akop.homesystem.models.color;

import lombok.Data;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import java.math.BigDecimal;
import java.math.MathContext;
import java.util.List;

import static ch.obermuhlner.math.big.BigDecimalMath.pow;
import static java.math.RoundingMode.HALF_UP;

/**
 * Represents an RGB-color.
 */
@SuppressWarnings("unused")
@Data
public class Color {

    public static final BigDecimal ZERO_POINT_55 = BigDecimal.valueOf(0.055);
    public static final BigDecimal TWO_POINT_4 = BigDecimal.valueOf(2.4);
    public static final BigDecimal TWELFE_POINT_92 = BigDecimal.valueOf(12.92);
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

    public static BigDecimal normalize(Integer value) {
        var maxValue = BigDecimal.valueOf(255);
        return BigDecimal.valueOf(value).divide(maxValue, HALF_UP);
    }

    public static BigDecimal makeVivid(BigDecimal value) {
        if (value.compareTo(BigDecimal.valueOf(0.04045)) > 0) {
            return pow(value.add(ZERO_POINT_55)
                    .divide(BigDecimal.ONE.add(ZERO_POINT_55), HALF_UP), TWO_POINT_4, MathContext.DECIMAL64);
        } else {
            return value.divide(TWELFE_POINT_92, HALF_UP);
        }
    }

    public List<BigDecimal> toXY() {
        // For the hue bulb the corners of the triangle are:
        // -Red: 0.675, 0.322
        // -Green: 0.4091, 0.518
        // -Blue: 0.167, 0.04

        var normalizedRed = normalize(getR());
        var normalizedGreen = normalize(getG());
        var normalizedBlue = normalize(getB());

        var red = makeVivid(normalizedRed);
        var green = makeVivid(normalizedGreen);
        var blue = makeVivid(normalizedBlue);

        var X = red.multiply(BigDecimal.valueOf(0.649926)).add(green.multiply(BigDecimal.valueOf(0.103455))).add(blue.add(BigDecimal.valueOf(0.197109)));
        var Y = red.multiply(BigDecimal.valueOf(0.23427)).add(green.multiply(BigDecimal.valueOf(0.743075))).add(blue.multiply(BigDecimal.valueOf(0.022598)));
        var Z = green.multiply(BigDecimal.valueOf(0.0053077)).add(blue.multiply(BigDecimal.valueOf(1.035763)));

        var sum = X.add(Y).add(Z);

        var x = X.divide(sum, HALF_UP);
        var y = Y.divide(sum, HALF_UP);

        return List.of(x, y);
    }
}
