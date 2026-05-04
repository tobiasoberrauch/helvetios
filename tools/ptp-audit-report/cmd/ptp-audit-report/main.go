// CLI for the RTS-25 audit-pack generator (T217).
//
// Usage:
//
//	ptp-audit-report --input ptp-samples.csv --period "2026-Q2" --out audit.txt
//	ptp-audit-report --input ptp-samples.csv --period "2026-Q2" --out audit.pdf --format pdf
//	ptp-audit-report --opensearch http://os:9200 --index "ptp-logs-*" --period "2026-Q2" --out audit.pdf --format pdf
package main

import (
	"flag"
	"fmt"
	"os"
	"strings"
	"time"

	"github.com/tobiasoberrauch/swisstms/ptp-audit-report/internal/audit"
	"github.com/tobiasoberrauch/swisstms/ptp-audit-report/internal/render"
	"github.com/tobiasoberrauch/swisstms/ptp-audit-report/internal/source"
)

func main() {
	input := flag.String("input", "", "CSV file with header server,iso_timestamp,offset_ns")
	osURL := flag.String("opensearch", "", "OpenSearch base URL; if set, --input is ignored")
	osIndex := flag.String("index", "ptp-logs-*", "OpenSearch index pattern")
	osUser := flag.String("os-user", "", "OpenSearch basic-auth user (optional)")
	osPass := flag.String("os-pass", "", "OpenSearch basic-auth password (optional)")
	period := flag.String("period", "", "Reporting period label, e.g. '2026-Q2'")
	out := flag.String("out", "audit.txt", "Output file path")
	format := flag.String("format", "text", "Output format: text|pdf")
	budget := flag.Int64("budget-ns", 100_000, "RTS-25 budget in nanoseconds (default 100µs)")
	flag.Parse()

	if *period == "" {
		fmt.Fprintln(os.Stderr, "ERROR: --period is required")
		flag.Usage()
		os.Exit(2)
	}
	if *input == "" && *osURL == "" {
		fmt.Fprintln(os.Stderr, "ERROR: either --input or --opensearch must be set")
		os.Exit(2)
	}

	samples, err := loadSamples(*input, *osURL, *osIndex, *osUser, *osPass)
	if err != nil {
		fmt.Fprintf(os.Stderr, "load samples: %v\n", err)
		os.Exit(1)
	}

	stats := audit.ComputePerServer(samples, *budget)
	if len(stats) == 0 {
		fmt.Fprintln(os.Stderr, "no samples loaded — refusing to write empty audit pack")
		os.Exit(1)
	}

	w, err := os.Create(*out)
	if err != nil {
		fmt.Fprintf(os.Stderr, "create output: %v\n", err)
		os.Exit(1)
	}
	defer w.Close()

	switch strings.ToLower(*format) {
	case "pdf":
		err = render.RenderPdf(w, *period, stats, time.Now())
	case "text", "txt":
		err = render.RenderText(w, *period, stats, time.Now())
	default:
		fmt.Fprintf(os.Stderr, "unknown --format %q (text|pdf)\n", *format)
		os.Exit(2)
	}
	if err != nil {
		fmt.Fprintf(os.Stderr, "render: %v\n", err)
		os.Exit(1)
	}

	fmt.Printf("✓ wrote %s — %d servers, %d samples\n", *out, len(stats), len(samples))
	for _, s := range stats {
		if !s.WithinRtsBudget {
			fmt.Printf("  ⚠  %s exceeded RTS-25 budget (max %s)\n", s.Server, audit.FormatNs(s.MaxAbsNs))
		}
	}
}

func loadSamples(file, osURL, osIndex, user, pass string) ([]audit.Sample, error) {
	if osURL != "" {
		return source.LoadOpenSearch(source.OpenSearchSource{
			BaseURL:   osURL,
			IndexGlob: osIndex,
			Username:  user,
			Password:  pass,
		})
	}
	in, err := os.Open(file)
	if err != nil {
		return nil, err
	}
	defer in.Close()
	return source.LoadCsv(in)
}
