// Minimal pure-Go PDF generator for RTS-25 audit packs (T216).
//
// Produces a valid PDF 1.4 with a single page containing the same monospaced text the text
// renderer emits. The footer carries the SHA-256 of the rendered text body so cosign-style
// detached signatures can be verified against the document content.
//
// We deliberately avoid external dependencies (no jung-kurt/gofpdf, no go-pdf/fpdf) so the
// audit-pack tool stays buildable on any Go 1.22 host without a network. The PDF is intentionally
// boring — a single Courier font at 9pt; no images, no embedded fonts, no encryption.
package render

import (
	"bytes"
	"crypto/sha256"
	"encoding/hex"
	"fmt"
	"io"
	"strings"
	"time"

	"github.com/tobiasoberrauch/swisstms/ptp-audit-report/internal/audit"
)

// RenderPdf writes a single-page PDF version of the audit pack.
func RenderPdf(w io.Writer, period string, stats []audit.ServerStats, generatedAt time.Time) error {
	body := buildBody(period, stats, generatedAt)
	digest := sha256.Sum256([]byte(body))
	bodyWithSig := body + fmt.Sprintf("\n--- Signature ---\nHash:  sha256:%s\nTool:  swisstms-ptp-audit-report\n",
		hex.EncodeToString(digest[:]))

	stream := buildContentStream(bodyWithSig)
	pdf, err := assemblePdf(stream)
	if err != nil {
		return err
	}
	_, err = w.Write(pdf)
	return err
}

func buildBody(period string, stats []audit.ServerStats, generatedAt time.Time) string {
	var sb strings.Builder
	fmt.Fprintf(&sb, "Swiss-TMS Platform — RTS-25 Time-Sync Audit Pack\n")
	fmt.Fprintf(&sb, "=================================================\n")
	fmt.Fprintf(&sb, "Reporting period: %s\n", period)
	fmt.Fprintf(&sb, "Generated at:     %s\n", generatedAt.UTC().Format(time.RFC3339Nano))
	fmt.Fprintf(&sb, "Servers:          %d\n\n", len(stats))
	fmt.Fprintf(&sb, "%-28s %10s %12s %12s %12s %10s\n",
		"Server", "Samples", "Median", "p99", "Max", "Budget?")
	fmt.Fprintf(&sb, "%s\n", strings.Repeat("-", 88))
	for _, s := range stats {
		ok := "OK"
		if !s.WithinRtsBudget {
			ok = "VIOLATION"
		}
		fmt.Fprintf(&sb, "%-28s %10d %12s %12s %12s %10s\n",
			s.Server,
			s.SampleCount,
			audit.FormatNs(s.MedianAbsNs),
			audit.FormatNs(s.P99AbsNs),
			audit.FormatNs(s.MaxAbsNs),
			ok,
		)
	}
	if len(stats) > 0 {
		fmt.Fprintf(&sb, "\nRTS-25 budget: %s (≤100µs for trading servers).\n",
			audit.FormatNs(stats[0].BudgetNs))
	}
	return sb.String()
}

// buildContentStream produces a minimal Tj-text content stream that lays each line of `body`
// on its own line at 9pt Courier, top-down from y=780.
func buildContentStream(body string) []byte {
	var sb strings.Builder
	sb.WriteString("BT\n/F1 9 Tf\n12 TL\n40 780 Td\n")
	for _, line := range strings.Split(body, "\n") {
		sb.WriteString("(")
		sb.WriteString(escapePdfText(line))
		sb.WriteString(") Tj\nT*\n")
	}
	sb.WriteString("ET\n")
	return []byte(sb.String())
}

func escapePdfText(s string) string {
	// Escape backslashes, parens, and replace non-ASCII (PDF 1.4 PDFDocEncoding fallback).
	r := strings.NewReplacer(
		`\`, `\\`,
		`(`, `\(`,
		`)`, `\)`,
	)
	out := r.Replace(s)
	var b strings.Builder
	for _, c := range out {
		if c < 0x20 || c > 0x7e {
			b.WriteByte('?')
		} else {
			b.WriteRune(c)
		}
	}
	return b.String()
}

// assemblePdf builds a 5-object PDF: Catalog, Pages, Page, Font, ContentStream.
func assemblePdf(content []byte) ([]byte, error) {
	var buf bytes.Buffer
	offsets := make([]int, 6)

	if _, err := buf.WriteString("%PDF-1.4\n%âãÏÓ\n"); err != nil {
		return nil, err
	}

	writeObj := func(idx int, body string) error {
		offsets[idx] = buf.Len()
		_, err := fmt.Fprintf(&buf, "%d 0 obj\n%s\nendobj\n", idx, body)
		return err
	}

	if err := writeObj(1, "<< /Type /Catalog /Pages 2 0 R >>"); err != nil {
		return nil, err
	}
	if err := writeObj(2, "<< /Type /Pages /Count 1 /Kids [3 0 R] >>"); err != nil {
		return nil, err
	}
	if err := writeObj(3,
		"<< /Type /Page /Parent 2 0 R /MediaBox [0 0 612 792] "+
			"/Resources << /Font << /F1 4 0 R >> >> /Contents 5 0 R >>"); err != nil {
		return nil, err
	}
	if err := writeObj(4, "<< /Type /Font /Subtype /Type1 /BaseFont /Courier /Encoding /WinAnsiEncoding >>"); err != nil {
		return nil, err
	}

	// Object 5 = content stream
	offsets[5] = buf.Len()
	fmt.Fprintf(&buf, "5 0 obj\n<< /Length %d >>\nstream\n", len(content))
	buf.Write(content)
	buf.WriteString("\nendstream\nendobj\n")

	xrefOffset := buf.Len()
	fmt.Fprintf(&buf, "xref\n0 6\n")
	fmt.Fprintf(&buf, "0000000000 65535 f \n")
	for i := 1; i <= 5; i++ {
		fmt.Fprintf(&buf, "%010d 00000 n \n", offsets[i])
	}
	fmt.Fprintf(&buf, "trailer\n<< /Size 6 /Root 1 0 R >>\nstartxref\n%d\n%%%%EOF\n", xrefOffset)

	return buf.Bytes(), nil
}
