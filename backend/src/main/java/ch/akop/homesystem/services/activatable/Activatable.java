package ch.akop.homesystem.services.activatable;


import io.reactivex.rxjava3.disposables.Disposable;
import java.util.ArrayList;
import java.util.List;

public abstract class Activatable implements Disposable {

  private final List<Disposable> runningDisposables = new ArrayList<>();

  protected void disposeWhenClosed(final Disposable disposable) {
    this.runningDisposables.add(disposable);
  }

  @Override
  public void dispose() {
    this.runningDisposables.forEach(Disposable::dispose);
    this.runningDisposables.clear();
  }

  @Override
  public boolean isDisposed() {
    return false;
  }

  public Disposable start() {
    this.started();
    return this;
  }

  protected abstract void started();

}
