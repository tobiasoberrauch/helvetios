package audit

import (
	"testing"
	"time"
)

func TestComputePerServer_BudgetExceeded(t *testing.T) {
	t0 := time.Date(2026, 5, 3, 0, 0, 0, 0, time.UTC)
	samples := []Sample{
		{Server: "node-1", BizTime: t0, OffsetNs: 50_000, OffsetAbs: 50_000},
		{Server: "node-1", BizTime: t0.Add(time.Second), OffsetNs: 75_000, OffsetAbs: 75_000},
		{Server: "node-1", BizTime: t0.Add(2 * time.Second), OffsetNs: 200_000, OffsetAbs: 200_000}, // > 100µs
	}
	stats := ComputePerServer(samples, 100_000) // RTS-25 budget = 100µs = 100_000ns
	if len(stats) != 1 {
		t.Fatalf("expected 1 server, got %d", len(stats))
	}
	s := stats[0]
	if s.Server != "node-1" {
		t.Fatalf("server name mismatch: %s", s.Server)
	}
	if s.MaxAbsNs != 200_000 {
		t.Fatalf("max should be 200_000, got %d", s.MaxAbsNs)
	}
	if s.WithinRtsBudget {
		t.Fatal("server with 200µs offset should NOT be within RTS-25 budget")
	}
}

func TestComputePerServer_AllInBudget(t *testing.T) {
	t0 := time.Date(2026, 5, 3, 0, 0, 0, 0, time.UTC)
	samples := []Sample{
		{Server: "node-1", BizTime: t0, OffsetAbs: 30_000},
		{Server: "node-1", BizTime: t0.Add(time.Second), OffsetAbs: 50_000},
	}
	stats := ComputePerServer(samples, 100_000)
	if !stats[0].WithinRtsBudget {
		t.Fatal("server with 50µs max should be within RTS-25 budget")
	}
}

func TestFormatNs(t *testing.T) {
	cases := map[int64]string{
		500:        "500ns",
		1500:       "1.50µs",
		1_500_000:  "1.500ms",
	}
	for in, want := range cases {
		got := FormatNs(in)
		if got != want {
			t.Errorf("FormatNs(%d) = %q, want %q", in, got, want)
		}
	}
}
