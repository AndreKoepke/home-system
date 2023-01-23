package ch.akop.homesystem.persistence.model.animation.steps;

import ch.akop.homesystem.models.devices.actor.DimmableLight;
import ch.akop.homesystem.persistence.model.animation.Animation;
import ch.akop.homesystem.services.DeviceService;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import org.hibernate.annotations.GenericGenerator;
import org.jetbrains.annotations.Nullable;

import javax.persistence.Entity;
import javax.persistence.EntityNotFoundException;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import java.math.BigDecimal;
import java.time.Duration;
import java.util.UUID;

@Entity
@Table(name = "animation_step_dimmer")
@Getter
@Setter
public class DimmLightStep implements Step {

    public static final Duration DEFAULT_DURATION = Duration.ofSeconds(10);

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
    private BigDecimal dimmLightTo;

    @Nullable
    private Duration dimmDuration;


    @Override
    public void play(DeviceService deviceService) {
        deviceService.findDeviceByName(nameOfLight, DimmableLight.class)
                .orElseThrow(() -> new EntityNotFoundException("Light with name " + nameOfLight + " was not found"))
                .setBrightness(dimmLightTo, dimmDuration != null ? dimmDuration : DEFAULT_DURATION);
    }
}
