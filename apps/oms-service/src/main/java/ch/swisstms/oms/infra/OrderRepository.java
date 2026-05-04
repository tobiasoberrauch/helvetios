package ch.swisstms.oms.infra;

import ch.swisstms.domain.order.OrdStatus;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface OrderRepository extends JpaRepository<OrderEntity, UUID> {

  Optional<OrderEntity> findByClientIdAndClOrdId(UUID clientId, String clOrdId);

  List<OrderEntity> findByClientIdAndOrdStatusIn(UUID clientId, List<OrdStatus> statuses);
}
