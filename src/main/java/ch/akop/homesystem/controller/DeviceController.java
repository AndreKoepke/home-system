package ch.akop.homesystem.controller;

import ch.akop.homesystem.controller.dtos.LightDto;
import ch.akop.homesystem.models.color.Color;
import ch.akop.homesystem.models.devices.actor.ColoredLight;
import ch.akop.homesystem.models.devices.actor.DimmableLight;
import ch.akop.homesystem.models.devices.actor.SimpleLight;
import ch.akop.homesystem.models.devices.sensor.MotionSensor;
import ch.akop.homesystem.services.DeviceService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.util.Collection;
import java.util.NoSuchElementException;

@RestController
@RequiredArgsConstructor
@CrossOrigin(origins = "*", allowedHeaders = "*")
public class DeviceController {

    private final DeviceService deviceService;

    @GetMapping(path = "/lights")
    public Collection<LightDto> getLights() {
        return deviceService.getDevicesOfType(ColoredLight.class).stream()
                .map(LightDto::from)
                .toList();
    }

    @PostMapping(path = "/lights/{id}/brightness")
    public void change(@PathVariable String id, @RequestBody int newBrightness) {
        deviceService.findDeviceById(id, DimmableLight.class)
                .orElseThrow(() -> new NoSuchElementException("Light not found"))
                .setBrightness(newBrightness, Duration.ofSeconds(10));
    }

    @PostMapping(path = "/lights/{id}/color")
    public void change(@PathVariable String id, @RequestBody Color newColor) {
        deviceService.findDeviceById(id, ColoredLight.class)
                .orElseThrow(() -> new NoSuchElementException("Light not found"))
                .setColor(newColor, Duration.ofSeconds(10));
    }

    @PostMapping(path = "/lights/{id}/switchTo")
    public void change(@PathVariable String id, @RequestBody Boolean newState) {
        deviceService.findDeviceById(id, SimpleLight.class)
                .orElseThrow(() -> new NoSuchElementException("Light not found"))
                .turnOn(newState);
    }

    @GetMapping(path = "/motionSensor")
    public Collection<MotionSensor> getMotionSensors() {
        return deviceService.getDevicesOfType(MotionSensor.class);
    }

}
