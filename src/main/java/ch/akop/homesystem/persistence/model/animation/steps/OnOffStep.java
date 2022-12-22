package ch.akop.homesystem.persistence.model.animation.steps;

import ch.akop.homesystem.models.devices.actor.SimpleLight;
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

import java.util.UUID;

@Entity
@Table(name = "animation_step_on_off")
@Getter
@Setter
@Configurable
public class OnOffStep implements Step {

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
    private Boolean turnItOn;


    @Override
    public void play() {
        deviceService.findDeviceByName(nameOfLight, SimpleLight.class)
                .orElseThrow(() -> new EntityNotFoundException("Light with name " + nameOfLight + " was not found"))
                .turnOn(turnItOn);
    }
}
