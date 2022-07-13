package ch.akop.homesystem.models.events;

import lombok.Data;

@Data
public class ButtonPressEvent {
    private final String buttonName;
    private final int buttonEvent;
}
