package ch.akop.homesystem.models.events;


public class ButtonPressInternalEvent extends ButtonPressEvent {

  public ButtonPressInternalEvent(String buttonName, int buttonEvent) {
    super(buttonName, buttonEvent);
  }
}
