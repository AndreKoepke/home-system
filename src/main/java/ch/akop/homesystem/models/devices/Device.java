package ch.akop.homesystem.models.devices;

import lombok.Data;
import lombok.ToString;

import java.time.LocalDateTime;

@SuppressWarnings("unchecked")
@Data
public abstract class Device<T> {

    private String name;
    private String id;
    
    @ToString.Exclude
    private LocalDateTime lastChange;


    public T setName(String name) {
        this.name = name;
        return (T) this;
    }

    public T setId(String id) {
        this.id = id;
        return (T) this;
    }

    public T setLastChange(LocalDateTime lastChange) {
        this.lastChange = lastChange;
        return (T) this;
    }

}
