package ch.akop.homesystem;

import lombok.SneakyThrows;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class HomeSystemApplication {

    @SneakyThrows
    public static void main(final String[] args) {
        SpringApplication.run(HomeSystemApplication.class, args);
    }

}
