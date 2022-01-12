package ch.akop.homesystem.deconz.rest.models;

import lombok.Data;

@Data
public class Scene {

    private String id;
    private String name;
    private int transitionTime;
    private int lightCount;

}
