package ch.akop.homesystem.persistence.model.animation;

import ch.akop.homesystem.persistence.model.animation.steps.DimmLightStep;
import ch.akop.homesystem.persistence.model.animation.steps.OnOffStep;
import ch.akop.homesystem.persistence.model.animation.steps.PauseStep;
import ch.akop.homesystem.persistence.model.animation.steps.Step;
import ch.akop.homesystem.services.DeviceService;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;

import java.util.ArrayList;
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
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @NonNull
    @OneToMany(cascade = CascadeType.ALL)
    private List<PauseStep> pauseSteps = new ArrayList<>();

    @NonNull
    @OneToMany(cascade = CascadeType.ALL)
    private List<OnOffStep> onOffSteps = new ArrayList<>();

    @NonNull
    @OneToMany(cascade = CascadeType.ALL)
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

}
