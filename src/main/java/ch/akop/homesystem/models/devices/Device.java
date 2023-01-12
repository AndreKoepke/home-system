package ch.akop.homesystem.models.devices;

import ch.akop.homesystem.deconz.rest.State;
import lombok.Data;

@SuppressWarnings("unchecked")
@Data
public abstract class Device<T extends Device<?>> {

    private String name;
    private String id;
    private boolean reachable;

    protected abstract void consumeInternalUpdate(State update);

    public T consumeUpdate(State update) {
        reachable = update.getReachable();
        consumeInternalUpdate(update);
        return (T) this;
    }

    //<editor-fold desc="These setter are necessary, because they're returning <T>. It is the type of the subclass.">
    public T setName(String name) {
        this.name = name;
        return (T) this;
    }

    public T setId(String id) {
        this.id = id;
        return (T) this;
    }
    //</editor-fold>
}
