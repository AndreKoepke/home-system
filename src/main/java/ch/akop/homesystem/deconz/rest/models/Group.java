package ch.akop.homesystem.deconz.rest.models;

import lombok.Data;

import java.util.List;

@Data
public class Group {

    private String name;
    private String id;
    private String type;
    private List<Scene> scenes;
}
