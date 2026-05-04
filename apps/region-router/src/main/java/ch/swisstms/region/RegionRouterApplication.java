package ch.swisstms.region;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class RegionRouterApplication {
  public static void main(String[] args) {
    SpringApplication.run(RegionRouterApplication.class, args);
  }
}
