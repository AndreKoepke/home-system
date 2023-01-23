package ch.akop.homesystem.persistence.model.config;

import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import org.hibernate.annotations.UpdateTimestamp;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(name = "config_mastodon")
@Getter
@Setter
public class MastodonConfig {

    @Id
    @UpdateTimestamp
    private LocalDateTime modified = LocalDateTime.now();

    @NonNull
    private String server;

    @NonNull
    private String token;
}
