package ch.swisstms.venue.six.sti;

import ch.swisstms.domain.execution.ExecutionId;
import ch.swisstms.domain.execution.LiquidityIndicator;
import ch.swisstms.domain.order.Order;
import ch.swisstms.domain.order.OrderId;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import quickfix.FieldNotFound;
import quickfix.Message;
import quickfix.field.AvgPx;
import quickfix.field.ClOrdID;
import quickfix.field.CumQty;
import quickfix.field.ExecID;
import quickfix.field.ExecType;
import quickfix.field.HandlInst;
import quickfix.field.LastPx;
import quickfix.field.LastQty;
import quickfix.field.LeavesQty;
import quickfix.field.OrdType;
import quickfix.field.OrderQty;
import quickfix.field.Price;
import quickfix.field.Side;
import quickfix.field.Symbol;
import quickfix.field.TimeInForce;
import quickfix.field.TransactTime;
import quickfix.fix44.NewOrderSingle;

/**
 * Domain ↔ FIX 4.4 (SIX STI Dialect) Mapper.
 *
 * <p>Constitution Principle I — alle FIX-Tags leben hier; aufrufender Code (OMS) sieht nur
 * Domain-Typen.
 */
public class SixStiMessageMapper {

  /** Konvertiert eine Domain-Order in FIX 4.4 NewOrderSingle (35=D). */
  public NewOrderSingle toNewOrderSingle(Order order) {
    NewOrderSingle msg =
        new NewOrderSingle(
            new ClOrdID(order.clOrdId().value()),
            new Side(toFixSide(order.side())),
            new TransactTime(
                java.time.LocalDateTime.ofInstant(
                    order.submittedAtBiz(), java.time.ZoneOffset.UTC)),
            new OrdType(toFixOrdType(order.ordType())));
    msg.set(new HandlInst((char) routingModeToHandlInst(order)));
    msg.set(new Symbol(order.instrument().mic() + ":" + order.instrument().isin()));
    msg.set(new OrderQty(order.orderQty().toBigDecimal().doubleValue()));
    if (!order.price().isMarket()) {
      msg.set(new Price(order.price().toBigDecimal().doubleValue()));
    }
    msg.set(new TimeInForce(toFixTimeInForce(order.timeInForce())));
    return msg;
  }

  /** Konvertiert eingehenden FIX 4.4 ExecutionReport (35=8) in Domain-ExecutionReport. */
  public ch.swisstms.domain.execution.ExecutionReport fromExecutionReport(
      quickfix.fix44.ExecutionReport msg) throws FieldNotFound {
    // OrderId — wir nutzen den ClOrdID-Wert (UUID-formatted) als OMS-OrderId.
    UUID orderUuid = UUID.fromString(msg.getString(ClOrdID.FIELD));
    UUID execUuid = UUID.nameUUIDFromBytes(msg.getString(ExecID.FIELD).getBytes());

    char fixExecType = msg.getChar(ExecType.FIELD);
    ch.swisstms.domain.execution.ExecType execType = mapExecType(fixExecType);

    BigDecimal lastQty = optDecimal(msg, LastQty.FIELD).orElse(BigDecimal.ZERO);
    BigDecimal lastPx = optDecimal(msg, LastPx.FIELD).orElse(BigDecimal.ZERO);
    BigDecimal cumQty = optDecimal(msg, CumQty.FIELD).orElse(BigDecimal.ZERO);
    BigDecimal leavesQty = optDecimal(msg, LeavesQty.FIELD).orElse(BigDecimal.ZERO);
    BigDecimal avgPx = optDecimal(msg, AvgPx.FIELD).orElse(BigDecimal.ZERO);

    Instant bizTime =
        msg.isSetField(TransactTime.FIELD)
            ? msg.getUtcTimeStamp(TransactTime.FIELD).atZone(java.time.ZoneOffset.UTC).toInstant()
            : Instant.now();
    Instant procTime = Instant.now();

    return new ch.swisstms.domain.execution.ExecutionReport(
        new ExecutionId(execUuid),
        msg.getString(ExecID.FIELD),
        new OrderId(orderUuid),
        execType,
        ch.swisstms.domain.price.Quantity.of(lastQty),
        lastPx.signum() == 0
            ? ch.swisstms.domain.price.Price.MARKET
            : ch.swisstms.domain.price.Price.of(lastPx),
        ch.swisstms.domain.price.Quantity.of(cumQty),
        ch.swisstms.domain.price.Quantity.of(leavesQty),
        avgPx.signum() == 0
            ? ch.swisstms.domain.price.Price.MARKET
            : ch.swisstms.domain.price.Price.of(avgPx),
        LiquidityIndicator.ADD, // SIX STI doesn't carry liquidity flag in stub
        "XSWX",
        bizTime,
        procTime);
  }

  private static java.util.Optional<BigDecimal> optDecimal(Message msg, int tag) {
    try {
      return msg.isSetField(tag)
          ? java.util.Optional.of(BigDecimal.valueOf(msg.getDouble(tag)))
          : java.util.Optional.empty();
    } catch (FieldNotFound e) {
      return java.util.Optional.empty();
    }
  }

  private static char toFixSide(ch.swisstms.domain.order.Side side) {
    return switch (side) {
      case BUY -> '1';
      case SELL -> '2';
      case SELL_SHORT -> '5';
    };
  }

  private static char toFixOrdType(ch.swisstms.domain.order.OrdType ot) {
    return switch (ot) {
      case MARKET -> '1';
      case LIMIT -> '2';
      case STOP -> '3';
      case STOP_LIMIT -> '4';
      case FUNARI -> 'I'; // SIX-Funari-Custom (CharOrdType 'I')
      case MOO -> '5';
      case LOO -> '5';
    };
  }

  private static char toFixTimeInForce(ch.swisstms.domain.order.TimeInForce tif) {
    return switch (tif) {
      case DAY -> '0';
      case GTC -> '1';
      case OPG -> '2';
      case IOC -> '3';
      case FOK -> '4';
      case GTD -> '6';
    };
  }

  private static int routingModeToHandlInst(Order order) {
    return switch (order.routingMode()) {
      case DMA -> 1;
      case ALGO_WHEEL -> 2;
      case CARE -> 3;
    };
  }

  private static ch.swisstms.domain.execution.ExecType mapExecType(char fixExecType) {
    return switch (fixExecType) {
      case '0' -> ch.swisstms.domain.execution.ExecType.NEW;
      case '1', 'F' -> ch.swisstms.domain.execution.ExecType.PARTIAL_FILL;
      case '2' -> ch.swisstms.domain.execution.ExecType.FILL;
      case '4' -> ch.swisstms.domain.execution.ExecType.CANCELED;
      case '5' -> ch.swisstms.domain.execution.ExecType.REPLACED;
      case '8' -> ch.swisstms.domain.execution.ExecType.REJECTED;
      case 'C' -> ch.swisstms.domain.execution.ExecType.EXPIRED;
      case 'H' -> ch.swisstms.domain.execution.ExecType.TRADE_BUST;
      default -> throw new IllegalArgumentException("Unknown FIX ExecType: " + fixExecType);
    };
  }
}
