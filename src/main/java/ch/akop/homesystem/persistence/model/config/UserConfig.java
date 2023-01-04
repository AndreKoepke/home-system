package ch.akop.homesystem.persistence.model.config;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;


@Entity
@Table(name = "config_user")
@Getter
@Setter
public class UserConfig {

    @Id
    @Column(columnDefinition = "TEXT")
    private String name;

    @NonNull
    @Column(columnDefinition = "TEXT")
    private String telegramId;

    @NonNull
    @Column(columnDefinition = "TEXT")
    private String deviceIp;

    private boolean dev = false;

}
