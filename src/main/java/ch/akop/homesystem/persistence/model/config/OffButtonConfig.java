package ch.akop.homesystem.persistence.model.config;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;

@Entity
@Table(name = "config_off_button")
@Getter
@Setter
public class OffButtonConfig {

    @Id
    @Column(columnDefinition = "TEXT")
    private String name;

    @NonNull
    private Integer buttonEvent;

}
