package ch.akop.homesystem.models.animation;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

@Data
@Slf4j
abstract class AnimationStep {

    abstract void play();

}
