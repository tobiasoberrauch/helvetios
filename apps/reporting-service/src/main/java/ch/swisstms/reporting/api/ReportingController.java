package ch.swisstms.reporting.api;

import ch.swisstms.reporting.emir.EmirDtccGtrJob;
import ch.swisstms.reporting.emir.EmirRegisTrJob;
import ch.swisstms.reporting.finfrag.FinfraGArt39Job;
import ch.swisstms.reporting.rts22.Rts22Job;
import ch.swisstms.reporting.traxapa.TraxApaJob;
import java.time.LocalDate;
import java.util.Map;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/reports")
public class ReportingController {

  private final FinfraGArt39Job finfraG;
  private final Rts22Job rts22;
  private final TraxApaJob trax;
  private final EmirDtccGtrJob dtcc;
  private final EmirRegisTrJob regis;

  public ReportingController(
      FinfraGArt39Job f, Rts22Job r, TraxApaJob t, EmirDtccGtrJob dtcc, EmirRegisTrJob regis) {
    this.finfraG = f;
    this.rts22 = r;
    this.trax = t;
    this.dtcc = dtcc;
    this.regis = regis;
  }

  @PostMapping("/{reportType}/run")
  public Map<String, String> trigger(
      @PathVariable String reportType,
      @RequestParam(required = false) LocalDate reportingDate,
      @RequestParam(defaultValue = "false") boolean dryRun) {
    LocalDate date = reportingDate == null ? LocalDate.now().minusDays(1) : reportingDate;
    if (!dryRun) {
      switch (reportType) {
        case "FINFRAG_ART39" -> finfraG.run(date);
        case "RTS22" -> rts22.run(date);
        case "TRAX_APA" -> trax.run(date);
        case "EMIR_DTCC" -> dtcc.run(date);
        case "EMIR_REGIS" -> regis.run(date);
        default -> throw new IllegalArgumentException("Unknown reportType " + reportType);
      }
    }
    return Map.of(
        "reportType",
        reportType,
        "date",
        date.toString(),
        "dryRun",
        String.valueOf(dryRun),
        "status",
        "ACCEPTED");
  }
}
