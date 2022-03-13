package ch.akop.homesystem.states;

public interface State {

    void entered();

    void leave();

    void event(Event event);

    void event(String buttonName, int buttonEvent);

}
