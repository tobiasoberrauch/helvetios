package render

import (
	"bytes"
	"crypto/sha256"
	"encoding/hex"
	"strings"
	"testing"
	"time"

	"github.com/tobiasoberrauch/swisstms/ptp-audit-report/internal/audit"
)

// T212 — modify a byte in the audit pack body, assert the embedded SHA-256 footer no longer
// matches the body. This is the plain-text equivalent of cosign signature verification: the
// footer hash MUST cover the body bit-for-bit.
func TestRenderText_TamperBreaksHash(t *testing.T) {
	stats := []audit.ServerStats{
		{Server: "zh-trading-01", SampleCount: 5, MedianAbsNs: 30_000, P99AbsNs: 42_000, MaxAbsNs: 42_000, BudgetNs: 100_000, WithinRtsBudget: true},
	}
	var buf bytes.Buffer
	if err := RenderText(&buf, "2026-Q2", stats, time.Date(2026, 5, 4, 10, 0, 0, 0, time.UTC)); err != nil {
		t.Fatalf("RenderText: %v", err)
	}
	rendered := buf.String()

	// Extract the embedded hash from the footer.
	const tag = "Hash:  sha256:"
	idx := strings.Index(rendered, tag)
	if idx < 0 {
		t.Fatal("rendered audit pack missing Hash footer")
	}
	embedded := rendered[idx+len(tag) : idx+len(tag)+64]

	// The embedded hash should match a clean recompute of the pre-footer body.
	bodyEnd := strings.Index(rendered, "\n--- Signature ---")
	if bodyEnd < 0 {
		t.Fatal("missing signature section")
	}
	body := rendered[:bodyEnd]
	clean := sha256.Sum256([]byte(body))
	if hex.EncodeToString(clean[:]) != embedded {
		t.Fatalf("baseline hash mismatch — got %s want %s", hex.EncodeToString(clean[:]), embedded)
	}

	// Tamper with one byte in the body. The recomputed hash MUST differ → signature invalid.
	tampered := []byte(body)
	if len(tampered) < 100 {
		t.Fatal("body unexpectedly small")
	}
	if tampered[80] == 'X' {
		tampered[80] = 'Y'
	} else {
		tampered[80] = 'X'
	}
	tamperedHash := sha256.Sum256(tampered)
	if hex.EncodeToString(tamperedHash[:]) == embedded {
		t.Fatal("tampered body produced identical hash — signature would NOT detect the change")
	}
}
