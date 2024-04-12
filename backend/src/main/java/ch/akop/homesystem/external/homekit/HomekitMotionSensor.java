package ch.akop.homesystem.external.homekit;

import io.github.hapjava.accessories.MotionSensorAccessory;
import io.github.hapjava.characteristics.HomekitCharacteristicChangeCallback;
import java.util.concurrent.CompletableFuture;

public class HomekitMotionSensor implements MotionSensorAccessory {

  @Override
  public CompletableFuture<Boolean> getMotionDetected() {
    return null;
  }

  @Override
  public void subscribeMotionDetected(HomekitCharacteristicChangeCallback callback) {

  }

  @Override
  public void unsubscribeMotionDetected() {

  }

  @Override
  public int getId() {
    return 0;
  }

  @Override
  public CompletableFuture<String> getName() {
    return null;
  }

  @Override
  public void identify() {

  }

  @Override
  public CompletableFuture<String> getSerialNumber() {
    return null;
  }

  @Override
  public CompletableFuture<String> getModel() {
    return null;
  }

  @Override
  public CompletableFuture<String> getManufacturer() {
    return null;
  }

  @Override
  public CompletableFuture<String> getFirmwareRevision() {
    return null;
  }
}
