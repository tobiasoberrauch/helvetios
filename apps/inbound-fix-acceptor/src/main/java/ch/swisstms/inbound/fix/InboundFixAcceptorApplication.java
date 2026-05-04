package ch.swisstms.inbound.fix;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class InboundFixAcceptorApplication {
  public static void main(String[] args) {
    SpringApplication.run(InboundFixAcceptorApplication.class, args);
  }
}
