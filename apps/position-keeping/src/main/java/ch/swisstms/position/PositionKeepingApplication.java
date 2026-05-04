package ch.swisstms.position;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.kafka.annotation.EnableKafka;

@SpringBootApplication
@EnableKafka
public class PositionKeepingApplication {
  public static void main(String[] args) {
    SpringApplication.run(PositionKeepingApplication.class, args);
  }
}
