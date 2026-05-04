package ch.swisstms.domain.instrument;

import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Composite identifier (ISIN + MIC) for an instrument across all venues. The same ISIN can list at
 * multiple venues; the MIC disambiguates.
 *
 * <p>Validation:
 *
 * <ul>
 *   <li>ISIN: ISO 6166 — 2 country letters + 9 alphanumerics + 1 check digit.
 *   <li>MIC: ISO 10383 — 4 alphanumerics.
 * </ul>
 *
 * <p>Constitution Principle I: this is a domain value object — no venue- specific protocol details.
 */
public record InstrumentId(String isin, String mic) {

  private static final Pattern ISIN_PATTERN = Pattern.compile("^[A-Z]{2}[A-Z0-9]{9}[0-9]$");
  private static final Pattern MIC_PATTERN = Pattern.compile("^[A-Z0-9]{4}$");

  public InstrumentId {
    Objects.requireNonNull(isin, "isin");
    Objects.requireNonNull(mic, "mic");
    if (!ISIN_PATTERN.matcher(isin).matches()) {
      throw new IllegalArgumentException("Invalid ISIN format: " + isin);
    }
    if (!MIC_PATTERN.matcher(mic).matches()) {
      throw new IllegalArgumentException("Invalid MIC format: " + mic);
    }
    if (!isinCheckDigitValid(isin)) {
      throw new IllegalArgumentException("Invalid ISIN check digit: " + isin);
    }
  }

  /** Luhn-style ISIN check-digit verification (ISO 6166). */
  private static boolean isinCheckDigitValid(String isin) {
    StringBuilder digits = new StringBuilder();
    for (int i = 0; i < isin.length() - 1; i++) {
      char c = isin.charAt(i);
      if (Character.isDigit(c)) {
        digits.append(c);
      } else {
        int val = Character.toUpperCase(c) - 'A' + 10;
        digits.append(val);
      }
    }
    int sum = 0;
    boolean dblFlag = true;
    for (int i = digits.length() - 1; i >= 0; i--) {
      int d = digits.charAt(i) - '0';
      if (dblFlag) {
        d *= 2;
        if (d > 9) d -= 9;
      }
      sum += d;
      dblFlag = !dblFlag;
    }
    int checkDigit = (10 - (sum % 10)) % 10;
    return checkDigit == (isin.charAt(isin.length() - 1) - '0');
  }
}
