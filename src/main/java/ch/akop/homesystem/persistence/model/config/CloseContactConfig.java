package ch.akop.homesystem.persistence.model.config;

import ch.akop.homesystem.persistence.model.animation.Animation;
import javax.annotation.Nullable;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.OneToOne;
import javax.persistence.Table;
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
