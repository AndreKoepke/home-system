package ch.akop.homesystem.persistence.model.config;

import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name = "config_off_button")
@Getter
@Setter
public class OffButtonConfig {

    @Id
    private String name;

    @NonNull
    private Integer buttonEvent;

}
