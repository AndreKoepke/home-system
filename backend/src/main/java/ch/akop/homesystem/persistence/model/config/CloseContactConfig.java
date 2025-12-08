package ch.akop.homesystem.persistence.model.config;

import ch.akop.homesystem.persistence.model.animation.Animation;
import jakarta.annotation.Nullable;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "config_close_contact")
@Getter
@Setter
public class CloseContactConfig {


    @Id
    private String name;

    @Nullable
    @Column(columnDefinition = "TEXT")
    private String messageWhenTrigger;

    @Nullable
    @OneToOne
    private Animation animationWhenTrigger;

}
