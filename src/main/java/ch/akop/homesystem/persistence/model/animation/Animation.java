package ch.akop.homesystem.persistence.model.animation;

import ch.akop.homesystem.persistence.model.animation.steps.DimmLightStep;
import ch.akop.homesystem.persistence.model.animation.steps.OnOffStep;
import ch.akop.homesystem.persistence.model.animation.steps.PauseStep;
import ch.akop.homesystem.persistence.model.animation.steps.Step;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

@Entity
@Table(name = "animation")
@Getter
@Setter
public class Animation {

    @Id
    private UUID id;

    @NonNull
    @OneToMany
    @OrderBy("sortOrder")
    private List<PauseStep> pauseSteps;

    @NonNull
    @OneToMany
    private List<OnOffStep> onOffSteps;

    @NonNull
    @OneToMany
    private List<DimmLightStep> dimmLightSteps;

    /**
     * Call in an async-context, this method can run for a while (blocking)
     */
    public void play() {
        Stream.of(pauseSteps.stream(), onOffSteps.stream(), dimmLightSteps.stream())
                .flatMap(stream -> stream)
                .sorted(Comparator.comparingInt(Step::getSortOrder))
                .forEachOrdered(Step::play);
    }

}
