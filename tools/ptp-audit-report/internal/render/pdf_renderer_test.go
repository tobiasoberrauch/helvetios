package render

import (
	"bytes"
	"strings"
	"testing"
	"time"

	"github.com/tobiasoberrauch/swisstms/ptp-audit-report/internal/audit"
)

func TestRenderPdf_ProducesValidPdf(t *testing.T) {
	stats := []audit.ServerStats{
		{Server: "zh-trading-01", SampleCount: 2, MedianAbsNs: 38_000, P99AbsNs: 42_000, MaxAbsNs: 42_000, BudgetNs: 100_000, WithinRtsBudget: true},
		{Server: "ny4-trading-01", SampleCount: 1, MedianAbsNs: 180_000, P99AbsNs: 180_000, MaxAbsNs: 180_000, BudgetNs: 100_000, WithinRtsBudget: false},
	}
	var buf bytes.Buffer
	if err := RenderPdf(&buf, "2026-Q2", stats, time.Date(2026, 5, 4, 10, 0, 0, 0, time.UTC)); err != nil {
		t.Fatalf("RenderPdf failed: %v", err)
	}
	out := buf.String()
	if !strings.HasPrefix(out, "%PDF-1.4") {
		t.Fatalf("output does not start with PDF magic; got: %q...", out[:min(20, len(out))])
	}
	if !strings.Contains(out, "%%EOF") {
		t.Fatal("output missing EOF trailer")
	}
	if !strings.Contains(out, "/Catalog") {
		t.Fatal("output missing /Catalog object")
	}
	if !strings.Contains(out, "Courier") {
		t.Fatal("output missing Courier font")
	}
}

func TestRenderPdf_FlagsViolations(t *testing.T) {
	stats := []audit.ServerStats{
		{Server: "ld4-edge-01", SampleCount: 1, MedianAbsNs: 250_000, P99AbsNs: 250_000, MaxAbsNs: 250_000, BudgetNs: 100_000, WithinRtsBudget: false},
	}
	var buf bytes.Buffer
	if err := RenderPdf(&buf, "2026-Q2", stats, time.Now()); err != nil {
		t.Fatal(err)
	}
	if !strings.Contains(buf.String(), "VIOLATION") {
		t.Fatal("budget violation not flagged in PDF text")
	}
}

func min(a, b int) int {
	if a < b {
		return a
	}
	return b
}
