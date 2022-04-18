package ch.akop.homesystem.models.config;

import lombok.Data;
import lombok.ToString;

@Data
@ToString(onlyExplicitlyIncluded = true)
public class User {

    @ToString.Include
    private String name;

    private String telegramId;
    private String deviceIp;
    private boolean dev;

}
