package ch.akop.homesystem.persistence.model.animation.steps;

import ch.akop.homesystem.models.devices.actor.SimpleLight;
import ch.akop.homesystem.persistence.model.animation.Animation;
import ch.akop.homesystem.services.impl.DeviceService;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityNotFoundException;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;

@Entity
@Table(name = "animation_step_on_off")
@Getter
@Setter
public class OnOffStep implements Step {

  @Id
  @GeneratedValue
  private UUID id;

  @NonNull
  private Integer sortOrder;

  @ManyToOne
  private Animation animation;

  @NonNull
  private String nameOfLight;

  @NonNull
  private Boolean turnItOn;


  @Override
  public void play(DeviceService deviceService) {
    deviceService.findDeviceByName(nameOfLight, SimpleLight.class)
        .orElseThrow(() -> new EntityNotFoundException("Light with name " + nameOfLight + " was not found"))
        .turnTo(turnItOn);
  }

  @Override
  public String getActionDescription() {
    return "schalte " + nameOfLight + (turnItOn ? " an" : " aus");
  }
}
