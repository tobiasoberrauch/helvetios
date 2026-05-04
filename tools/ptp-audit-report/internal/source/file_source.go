// Package source loads PTP/NTP samples from various input formats.
package source

import (
	"bufio"
	"fmt"
	"io"
	"strconv"
	"strings"
	"time"

	"github.com/tobiasoberrauch/swisstms/ptp-audit-report/internal/audit"
)

// LoadCsv parses a CSV file with header `server,iso_timestamp,offset_ns`.
// Used for offline audit-pack generation from exported logs.
func LoadCsv(r io.Reader) ([]audit.Sample, error) {
	scanner := bufio.NewScanner(r)
	scanner.Buffer(make([]byte, 1024*1024), 16*1024*1024)
	var samples []audit.Sample
	first := true
	lineNum := 0
	for scanner.Scan() {
		lineNum++
		line := strings.TrimSpace(scanner.Text())
		if line == "" {
			continue
		}
		if first {
			first = false
			if strings.Contains(strings.ToLower(line), "server") {
				continue
			}
		}
		parts := strings.Split(line, ",")
		if len(parts) != 3 {
			return nil, fmt.Errorf("line %d: expected 3 fields, got %d", lineNum, len(parts))
		}
		ts, err := time.Parse(time.RFC3339Nano, parts[1])
		if err != nil {
			return nil, fmt.Errorf("line %d: bad timestamp: %w", lineNum, err)
		}
		offset, err := strconv.ParseInt(parts[2], 10, 64)
		if err != nil {
			return nil, fmt.Errorf("line %d: bad offset: %w", lineNum, err)
		}
		abs := offset
		if abs < 0 {
			abs = -abs
		}
		samples = append(samples, audit.Sample{
			Server:    parts[0],
			BizTime:   ts,
			OffsetNs:  offset,
			OffsetAbs: abs,
		})
	}
	return samples, scanner.Err()
}
