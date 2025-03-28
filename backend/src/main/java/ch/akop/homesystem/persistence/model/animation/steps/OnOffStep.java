package ch.akop.homesystem.persistence.model.animation.steps;

import ch.akop.homesystem.models.devices.actor.SimpleLight;
import ch.akop.homesystem.persistence.model.animation.Animation;
import ch.akop.homesystem.services.impl.DeviceService;
import java.util.UUID;
import javax.persistence.Entity;
import javax.persistence.EntityNotFoundException;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import org.hibernate.annotations.GenericGenerator;

@Entity
@Table(name = "animation_step_on_off")
@Getter
@Setter
public class OnOffStep implements Step {

  @Id
  @GeneratedValue(generator = "UUID")
  @GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator")
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
