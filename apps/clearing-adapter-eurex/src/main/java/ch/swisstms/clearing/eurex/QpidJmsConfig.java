package ch.swisstms.clearing.eurex;

import jakarta.jms.ConnectionFactory;
import org.apache.qpid.jms.JmsConnectionFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jms.connection.CachingConnectionFactory;
import org.springframework.jms.core.JmsTemplate;

/**
 * Constitution research R-001 + R-008 — Apache Qpid JMS (AMQP 1.0).
 *
 * <p>Eurex hat AMQP 0-10 im Juni 2020 dekommissioniert; alle Mitglieder verwenden AMQP 1.0. Spring
 * CachingConnectionFactory (NICHT SingleConnectionFactory) für robuste Reconnect-Semantik.
 *
 * <p>JMS-Sessions sind nicht thread-safe — pro Worker-Thread wird genau eine Session aus dem Cache
 * geholt.
 */
@Configuration
public class QpidJmsConfig {

  @Bean
  public ConnectionFactory eurexConnectionFactory(
      @Value("${swisstms.eurex.amqp.url:amqps://eurex-amqp.local:5671}") String url,
      @Value("${swisstms.eurex.amqp.username:swisstms}") String user,
      @Value("${swisstms.eurex.amqp.password:#{null}}") String password) {
    JmsConnectionFactory raw = new JmsConnectionFactory(url);
    raw.setUsername(user);
    if (password != null) raw.setPassword(password);

    CachingConnectionFactory cache = new CachingConnectionFactory(raw);
    cache.setSessionCacheSize(8); // one per typical worker thread
    cache.setReconnectOnException(true);
    cache.setCacheConsumers(false); // explicit per-thread consumer creation
    return cache;
  }

  @Bean
  public JmsTemplate eurexJmsTemplate(ConnectionFactory eurexConnectionFactory) {
    JmsTemplate t = new JmsTemplate(eurexConnectionFactory);
    t.setReceiveTimeout(5000);
    t.setExplicitQosEnabled(true);
    t.setDeliveryPersistent(true);
    return t;
  }
}
