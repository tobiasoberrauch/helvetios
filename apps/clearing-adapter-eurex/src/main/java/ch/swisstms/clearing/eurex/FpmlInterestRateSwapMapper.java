package ch.swisstms.clearing.eurex;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import org.springframework.stereotype.Component;

/**
 * FpML 5.12 mapper for OTC Interest Rate Swaps cleared through Eurex C7 (US4 / FR-014).
 *
 * <p>Eurex C7 accepts FpML for OTC IRS clearing submission via the same AMQP queue as listed
 * derivatives, distinguished by the message wrapper. We support the minimum-viable {@code
 * <interestRateSwap>} payload: notional, fixed/float legs, schedule. A full FpML 5.12 surface will
 * be added in Phase 13 once we onboard non-Eurex CCPs that exchange OTCC trades.
 */
@Component
public class FpmlInterestRateSwapMapper {

  private static final DateTimeFormatter ISO_DATE = DateTimeFormatter.ISO_LOCAL_DATE;

  public record InterestRateSwap(
      String tradeId,
      LocalDate tradeDate,
      LocalDate effectiveDate,
      LocalDate terminationDate,
      BigDecimal notional,
      String currency,
      Leg fixedLeg,
      Leg floatingLeg) {}

  public record Leg(
      String payerPartyReference,
      String receiverPartyReference,
      BigDecimal rate,
      String floatingRateIndex,
      String dayCountFraction,
      String paymentFrequency) {}

  public String toFpmlXml(InterestRateSwap swap) {
    StringBuilder sb = new StringBuilder(1024);
    sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>")
        .append("<dataDocument xmlns=\"http://www.fpml.org/FpML-5/confirmation\"")
        .append(" fpmlVersion=\"5-12\">")
        .append("<trade>")
        .append("<tradeHeader>")
        .append("<partyTradeIdentifier>")
        .append("<partyReference href=\"swisstms\"/>")
        .append("<tradeId tradeIdScheme=\"swisstms-trade-id\">")
        .append(esc(swap.tradeId()))
        .append("</tradeId>")
        .append("</partyTradeIdentifier>")
        .append("<tradeDate>")
        .append(ISO_DATE.format(swap.tradeDate()))
        .append("</tradeDate>")
        .append("</tradeHeader>")
        .append("<swap>")
        .append(legXml("fixed", swap.fixedLeg(), swap, true))
        .append(legXml("floating", swap.floatingLeg(), swap, false))
        .append("</swap>")
        .append("</trade>")
        .append("</dataDocument>");
    return sb.toString();
  }

  private static String legXml(String kind, Leg leg, InterestRateSwap swap, boolean fixed) {
    StringBuilder s = new StringBuilder(256);
    s.append("<swapStream>")
        .append("<payerPartyReference href=\"")
        .append(esc(leg.payerPartyReference()))
        .append("\"/>")
        .append("<receiverPartyReference href=\"")
        .append(esc(leg.receiverPartyReference()))
        .append("\"/>")
        .append("<calculationPeriodDates id=\"")
        .append(kind)
        .append("CalcPeriod\">")
        .append("<effectiveDate><unadjustedDate>")
        .append(ISO_DATE.format(swap.effectiveDate()))
        .append("</unadjustedDate></effectiveDate>")
        .append("<terminationDate><unadjustedDate>")
        .append(ISO_DATE.format(swap.terminationDate()))
        .append("</unadjustedDate></terminationDate>")
        .append("</calculationPeriodDates>")
        .append("<calculationPeriodAmount><calculation>")
        .append("<notionalSchedule><notionalStepSchedule>")
        .append("<initialValue>")
        .append(swap.notional().toPlainString())
        .append("</initialValue>")
        .append("<currency>")
        .append(esc(swap.currency()))
        .append("</currency>")
        .append("</notionalStepSchedule></notionalSchedule>");
    if (fixed) {
      s.append("<fixedRateSchedule><initialValue>")
          .append(leg.rate().toPlainString())
          .append("</initialValue></fixedRateSchedule>");
    } else {
      s.append("<floatingRateCalculation><floatingRateIndex>")
          .append(esc(leg.floatingRateIndex()))
          .append("</floatingRateIndex></floatingRateCalculation>");
    }
    s.append("<dayCountFraction>")
        .append(esc(leg.dayCountFraction()))
        .append("</dayCountFraction>")
        .append("</calculation></calculationPeriodAmount>")
        .append("</swapStream>");
    return s.toString();
  }

  private static String esc(String s) {
    return s == null
        ? ""
        : s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
  }
}
