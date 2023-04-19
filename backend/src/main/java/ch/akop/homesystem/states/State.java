package ch.akop.homesystem.states;

public interface State {

  void entered(boolean quiet);

  void leave();

}
