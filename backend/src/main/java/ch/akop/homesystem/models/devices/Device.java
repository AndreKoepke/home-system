package ch.akop.homesystem.models.devices;

import ch.akop.homesystem.deconz.rest.State;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import lombok.Data;

@SuppressWarnings("unchecked")
@Data
public abstract class Device<T extends Device<?>> {

  private String name;
  private String id;
  private String uniqueId;
  private boolean reachable;
  private ZonedDateTime lastUpdated;

  protected abstract void consumeInternalUpdate(State update);

  public T consumeUpdate(State update) {
    reachable = update.getReachable() != null ? update.getReachable() : true;
    try {
      lastUpdated = LocalDateTime.parse(update.getLastupdated()).atZone(ZoneId.of("UTC"));
    } catch (Exception e) {
      lastUpdated = null;
    }
    consumeInternalUpdate(update);
    return (T) this;
  }

  //<editor-fold desc="These setter are necessary, because they're returning <T>. It is the type of the subclass.">
  public T setName(String name) {
    this.name = name;
    return (T) this;
  }

  public T setId(String id) {
    this.id = id;
    return (T) this;
  }

  public T setUniqueId(String uniqueId) {
    this.uniqueId = uniqueId;
    return (T) this;
  }
  //</editor-fold>
}
