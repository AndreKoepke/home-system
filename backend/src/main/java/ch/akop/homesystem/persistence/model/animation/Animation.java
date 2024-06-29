package ch.akop.homesystem.persistence.model.animation;

import ch.akop.homesystem.persistence.model.animation.steps.DimmLightStep;
import ch.akop.homesystem.persistence.model.animation.steps.OnOffStep;
import ch.akop.homesystem.persistence.model.animation.steps.PauseStep;
import ch.akop.homesystem.persistence.model.animation.steps.Step;
import ch.akop.homesystem.services.impl.DeviceService;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import org.hibernate.annotations.GenericGenerator;

@Entity
@Table(name = "animation")
@Getter
@Setter
public class Animation {

  @Id
  @GeneratedValue(generator = "UUID")
  @GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator")
  private UUID id;

  @NonNull
  @OneToMany(cascade = CascadeType.ALL, mappedBy = "animation")
  private List<PauseStep> pauseSteps = new ArrayList<>();

  @NonNull
  @OneToMany(cascade = CascadeType.ALL, mappedBy = "animation")
  private List<OnOffStep> onOffSteps = new ArrayList<>();

  @NonNull
  @OneToMany(cascade = CascadeType.ALL, mappedBy = "animation")
  private List<DimmLightStep> dimmLightSteps = new ArrayList<>();

  /**
   * Call in an async-context, this method can run for a while (blocking)
   */
  public void play(DeviceService deviceService) {
    Stream.of(pauseSteps.stream(), onOffSteps.stream(), dimmLightSteps.stream())
        .flatMap(stream -> stream)
        .sorted(Comparator.comparingInt(Step::getSortOrder))
        .forEachOrdered(step -> step.play(deviceService));
  }

  public Set<String> getLights() {
    return Stream.concat(
            onOffSteps.stream().map(OnOffStep::getNameOfLight),
            dimmLightSteps.stream().map(DimmLightStep::getNameOfLight)
        )
        .collect(Collectors.toSet());
  }
}
