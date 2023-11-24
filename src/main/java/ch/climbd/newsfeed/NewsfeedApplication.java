package ch.climbd.newsfeed;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class NewsfeedApplication {

    public static void main(String[] args) {
        SpringApplication.run(NewsfeedApplication.class, args);
    }
}
