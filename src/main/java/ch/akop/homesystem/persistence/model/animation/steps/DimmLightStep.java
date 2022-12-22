package ch.akop.homesystem.persistence.model.animation.steps;

import ch.akop.homesystem.models.devices.actor.DimmableLight;
import ch.akop.homesystem.persistence.model.animation.Animation;
import ch.akop.homesystem.services.DeviceService;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityNotFoundException;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.springframework.lang.Nullable;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.UUID;

@Entity
@Table(name = "animation_step_dimmer")
@Getter
@Setter
@Configurable
public class DimmLightStep implements Step {

    public static final Duration DEFAULT_DURATION = Duration.ofSeconds(10);

    @Autowired
    @Transient
    private DeviceService deviceService;

    @Id
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
    public void play() {
        deviceService.findDeviceByName(nameOfLight, DimmableLight.class)
                .orElseThrow(() -> new EntityNotFoundException("Light with name " + nameOfLight + " was not found"))
                .setBrightness(dimmLightTo, dimmDuration != null ? dimmDuration : DEFAULT_DURATION);
    }
}
