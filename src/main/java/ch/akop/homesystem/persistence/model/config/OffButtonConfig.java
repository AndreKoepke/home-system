package ch.akop.homesystem.persistence.model.config;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
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
