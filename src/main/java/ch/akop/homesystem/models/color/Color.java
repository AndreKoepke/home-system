package ch.akop.homesystem.models.color;

import lombok.Data;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;

/**
 * Represents an RGB-color.
 */
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
}
