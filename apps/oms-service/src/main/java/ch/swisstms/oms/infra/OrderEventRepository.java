package ch.swisstms.oms.infra;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface OrderEventRepository extends JpaRepository<OrderEventEntity, UUID> {

  List<OrderEventEntity> findByOrderIdOrderBySeqAsc(UUID orderId);

  Optional<OrderEventEntity> findTop1ByOrderIdOrderBySeqDesc(UUID orderId);
}
