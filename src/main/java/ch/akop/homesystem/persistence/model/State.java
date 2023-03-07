package ch.akop.homesystem.persistence.model;

import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(name = "state")
@Getter
@Setter
public class State {

    @Id
    @Column(name = "activated_at", nullable = false)
    private LocalDateTime activatedAt = LocalDateTime.now();

    @NonNull
    @Column(name = "class_name", nullable = false)
    private String className;
}
