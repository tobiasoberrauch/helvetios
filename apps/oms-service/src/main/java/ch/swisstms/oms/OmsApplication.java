package ch.swisstms.oms;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * OMS-Service Bootstrap.
 *
 * <p>Constitution Principle II — dieser Service liegt in der WARM-Tier (REST-/gRPC-API für
 * Trader-UI, Outbox→Kafka). Der Hot-Path (Aeron IPC) wird vom EMS-Service betrieben.
 */
@SpringBootApplication
@EnableKafka
@EnableScheduling
@EnableTransactionManagement
public class OmsApplication {
  public static void main(String[] args) {
    SpringApplication.run(OmsApplication.class, args);
  }
}
