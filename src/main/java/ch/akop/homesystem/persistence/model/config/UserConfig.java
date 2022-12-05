package ch.akop.homesystem.persistence.model.config;

import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

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
