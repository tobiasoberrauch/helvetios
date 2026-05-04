// Package audit computes per-server divergence statistics for the
// RTS-25 audit pack.
package audit

import (
	"fmt"
	"sort"
	"time"
)

// Sample is a single PTP/NTP divergence reading.
type Sample struct {
	Server     string
	BizTime    time.Time
	OffsetNs   int64 // signed: positive = ahead of UTC, negative = behind
	OffsetAbs  int64 // |OffsetNs| for percentile calc
}

// ServerStats summarises one server's divergence over the audit period.
type ServerStats struct {
	Server      string
	SampleCount int
	MedianAbsNs int64
	P99AbsNs    int64
	MaxAbsNs    int64
	WithinRtsBudget bool   // RTS-25: ≤100µs for trading servers
	BudgetNs    int64
}

// ComputePerServer returns stats per server. Zero-sample servers are
// emitted with WithinRtsBudget=false so the audit catches them.
func ComputePerServer(samples []Sample, budgetNs int64) []ServerStats {
	by := map[string][]int64{}
	for _, s := range samples {
		by[s.Server] = append(by[s.Server], s.OffsetAbs)
	}
	servers := make([]string, 0, len(by))
	for k := range by {
		servers = append(servers, k)
	}
	sort.Strings(servers)

	out := make([]ServerStats, 0, len(servers))
	for _, server := range servers {
		offsets := by[server]
		sort.Slice(offsets, func(i, j int) bool { return offsets[i] < offsets[j] })
		stats := ServerStats{
			Server:      server,
			SampleCount: len(offsets),
			BudgetNs:    budgetNs,
		}
		if len(offsets) > 0 {
			stats.MedianAbsNs = percentile(offsets, 0.5)
			stats.P99AbsNs    = percentile(offsets, 0.99)
			stats.MaxAbsNs    = offsets[len(offsets)-1]
			stats.WithinRtsBudget = stats.MaxAbsNs <= budgetNs
		}
		out = append(out, stats)
	}
	return out
}

func percentile(sortedAsc []int64, p float64) int64 {
	if len(sortedAsc) == 0 {
		return 0
	}
	idx := int(float64(len(sortedAsc)-1) * p)
	return sortedAsc[idx]
}

// FormatNs returns a human-readable representation (µs / ms).
func FormatNs(ns int64) string {
	if ns < 1000 {
		return fmt.Sprintf("%dns", ns)
	}
	if ns < 1_000_000 {
		return fmt.Sprintf("%.2fµs", float64(ns)/1000.0)
	}
	return fmt.Sprintf("%.3fms", float64(ns)/1_000_000.0)
}
