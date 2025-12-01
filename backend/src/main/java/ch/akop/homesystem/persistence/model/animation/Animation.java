package ch.akop.homesystem.persistence.model.animation;

import ch.akop.homesystem.persistence.model.animation.steps.DimmLightStep;
import ch.akop.homesystem.persistence.model.animation.steps.OnOffStep;
import ch.akop.homesystem.persistence.model.animation.steps.PauseStep;
import ch.akop.homesystem.persistence.model.animation.steps.Step;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import org.jetbrains.annotations.Nullable;

@Entity
@Table(name = "animation")
@Getter
@Setter
public class Animation {

  @Id
  @GeneratedValue
  private UUID id;

  @Nullable
  private String name;


  @NonNull
  @OneToMany(cascade = CascadeType.ALL, mappedBy = "animation")
  private List<PauseStep> pauseSteps = new ArrayList<>();

  @NonNull
  @OneToMany(cascade = CascadeType.ALL, mappedBy = "animation")
  private List<OnOffStep> onOffSteps = new ArrayList<>();

  @NonNull
  @OneToMany(cascade = CascadeType.ALL, mappedBy = "animation")
  private List<DimmLightStep> dimmLightSteps = new ArrayList<>();

  public List<? extends Step> materializeSteps() {
    return Stream.of(pauseSteps.stream(), onOffSteps.stream(), dimmLightSteps.stream())
        .flatMap(stream -> stream)
        .sorted(Comparator.comparingInt(Step::getSortOrder))
        .toList();
  }

  public Set<String> getLights() {
    return Stream.concat(
            onOffSteps.stream().map(OnOffStep::getNameOfLight),
            dimmLightSteps.stream().map(DimmLightStep::getNameOfLight)
        )
        .collect(Collectors.toSet());
  }
}
