// Package render produces human-readable + signed audit-pack reports.
package render

import (
	"crypto/sha256"
	"encoding/hex"
	"fmt"
	"io"
	"time"

	"github.com/tobiasoberrauch/swisstms/ptp-audit-report/internal/audit"
)

// RenderText writes a human-readable, signature-friendly text report
// suitable for FINMA audit submission. Signature uses SHA-256 of the
// report body, written as the last line. (cosign signing happens in
// the CLI driver — see cmd/ptp-audit-report/main.go.)
func RenderText(w io.Writer, period string, stats []audit.ServerStats, generatedAt time.Time) error {
	header := fmt.Sprintf(
		"Swiss-TMS Platform — RTS-25 Time-Sync Audit Pack\n"+
			"=================================================\n"+
			"Reporting period: %s\n"+
			"Generated at:     %s\n"+
			"Servers:          %d\n\n",
		period,
		generatedAt.UTC().Format(time.RFC3339Nano),
		len(stats),
	)
	if _, err := io.WriteString(w, header); err != nil {
		return err
	}

	body := ""
	body += fmt.Sprintf("%-32s %12s %14s %14s %14s %10s\n",
		"Server", "Samples", "Median |Δ|", "p99 |Δ|", "Max |Δ|", "Budget?")
	body += fmt.Sprintf("%s\n",
		"--------------------------------------------------------------------------------------------------")
	for _, s := range stats {
		ok := "OK"
		if !s.WithinRtsBudget {
			ok = "VIOLATION"
		}
		body += fmt.Sprintf("%-32s %12d %14s %14s %14s %10s\n",
			s.Server,
			s.SampleCount,
			audit.FormatNs(s.MedianAbsNs),
			audit.FormatNs(s.P99AbsNs),
			audit.FormatNs(s.MaxAbsNs),
			ok,
		)
	}
	body += fmt.Sprintf("\nRTS-25 budget: %s (≤100µs for trading servers).\n",
		audit.FormatNs(stats[0].BudgetNs))

	if _, err := io.WriteString(w, body); err != nil {
		return err
	}

	digest := sha256.Sum256([]byte(header + body))
	footer := fmt.Sprintf(
		"\n--- Signature ---\n"+
			"Hash:  sha256:%s\n"+
			"Tool:  swisstms-ptp-audit-report\n"+
			"Note:  This text is signed with cosign — see *.sig file\n",
		hex.EncodeToString(digest[:]),
	)
	_, err := io.WriteString(w, footer)
	return err
}
