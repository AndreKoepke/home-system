package ch.akop.homesystem.persistence.model;

import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;

import javax.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "openai_images")
@Getter
@Setter
public class ImageOfOpenAI {

    @Id
    private LocalDateTime created = LocalDateTime.now();

    @Column(nullable = false)
    private int downloaded = 0;

    @Column(nullable = false)
    @NonNull
    private String prompt;

    @Lob
    private byte[] image;

}
