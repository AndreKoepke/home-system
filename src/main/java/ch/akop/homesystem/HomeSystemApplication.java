package ch.akop.homesystem;

import lombok.SneakyThrows;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableJpaRepositories
@EnableScheduling
@EnableAsync
@ConfigurationPropertiesScan("ch.akop.homesystem.config.properties")
public class HomeSystemApplication {

    @SneakyThrows
    public static void main(String[] args) {
        SpringApplication.run(HomeSystemApplication.class, args);
    }

}
