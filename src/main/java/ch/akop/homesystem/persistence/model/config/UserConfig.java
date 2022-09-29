package ch.akop.homesystem.persistence.model.config;

import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name = "config_user")
@Getter
@Setter
public class UserConfig {

    @Id
    private String name;

    @NonNull
    private String telegramId;

    @NonNull
    private String deviceIp;

    private boolean dev = false;

}
