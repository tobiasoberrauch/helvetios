package ch.swisstms.ems.algo;

import ch.swisstms.domain.order.Order;
import ch.swisstms.domain.price.Price;
import ch.swisstms.domain.price.Quantity;
import java.time.Duration;
import java.util.List;

public interface AlgoStrategy {
  List<ChildSlice> sliceParent(Order parent);

  String name();

  record ChildSlice(Quantity quantity, Price price, Duration delay) {}
}
