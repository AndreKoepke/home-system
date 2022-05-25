package ch.akop.homesystem.models.devices.actor;

import ch.akop.homesystem.models.color.Color;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import org.apache.logging.log4j.util.TriConsumer;
import org.reactivestreams.Subscription;

import java.time.Duration;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
public class ColoredLight extends DimmableLight {

    private final TriConsumer<Integer, Color, Duration> functionForRgb;
    private Subscription loopSubscription;

    public ColoredLight(BiConsumer<Integer, Duration> functionToSeBrightness,
                        Consumer<Boolean> functionToTurnOnOrOff,
                        TriConsumer<Integer, Color, Duration> functionForRgb) {
        super.setFunctionToSeBrightness(patchConsumerWithStopTheLoop(functionToSeBrightness));
        super.setFunctionToTurnOnOrOff(patchConsumerWithStopTheLoop(functionToTurnOnOrOff));
        this.functionForRgb = patchConsumerWithStopTheLoop(functionForRgb);
    }

    private TriConsumer<Integer, Color, Duration> patchConsumerWithStopTheLoop(TriConsumer<Integer, Color, Duration> functionForRgb) {
        return (integer, color, duration) -> {
            stopTheLoop();
            functionForRgb.accept(integer, color, duration);
        };
    }

    private BiConsumer<Integer, Duration> patchConsumerWithStopTheLoop(BiConsumer<Integer, Duration> functionForBrightness) {
        return (integer, duration) -> {
            stopTheLoop();
            functionForBrightness.accept(integer, duration);
        };
    }
    private Consumer<Boolean> patchConsumerWithStopTheLoop(Consumer<Boolean> functionForOnOff) {
        return onOff -> {
            stopTheLoop();
            functionForOnOff.accept(onOff);
        };
    }


    private void stopTheLoop() {
        if (loopSubscription != null) {
            this.loopSubscription.cancel();
            this.loopSubscription = null;
        }
    }
}
