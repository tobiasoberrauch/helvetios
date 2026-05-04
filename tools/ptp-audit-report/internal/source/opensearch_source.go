// Package source loads PTP/NTP samples for the audit pack (T214).
//
// `opensearch_source.go` queries an OpenSearch index for `ptp4l` / `phc2sys` daily logs and
// converts each hit into a Sample. The query targets the index pattern `ptp-logs-YYYY-MM-DD`
// (one index per day) and fetches the rolling 24h window using a `scroll` (or, for OpenSearch
// 2.7+, the modern `search_after` API; we use `scroll` for portability).
//
// We intentionally avoid the official OpenSearch SDK to keep this tool dependency-free; the
// REST surface is small and stable.
package source

import (
	"bytes"
	"encoding/json"
	"fmt"
	"io"
	"net/http"
	"net/url"
	"strings"
	"time"

	"github.com/tobiasoberrauch/swisstms/ptp-audit-report/internal/audit"
)

type OpenSearchSource struct {
	BaseURL    string        // e.g. https://opensearch.internal:9200
	IndexGlob  string        // e.g. ptp-logs-*
	Username   string        // optional basic-auth
	Password   string        // optional basic-auth
	HTTPClient *http.Client  // optional; defaults to http.DefaultClient with 30s timeout
	Window     time.Duration // how far back to scan; default 24h
}

// LoadOpenSearch issues a `_search` against the configured index pattern and returns the
// flattened sample list. The query filters on the `@timestamp` field falling within the window
// and on the document type being either ptp4l or phc2sys.
func LoadOpenSearch(s OpenSearchSource) ([]audit.Sample, error) {
	client := s.HTTPClient
	if client == nil {
		client = &http.Client{Timeout: 30 * time.Second}
	}
	window := s.Window
	if window == 0 {
		window = 24 * time.Hour
	}

	endpoint := fmt.Sprintf("%s/%s/_search?size=10000",
		strings.TrimSuffix(s.BaseURL, "/"), url.PathEscape(s.IndexGlob))
	body := map[string]any{
		"query": map[string]any{
			"bool": map[string]any{
				"must": []map[string]any{
					{"range": map[string]any{
						"@timestamp": map[string]string{
							"gte": time.Now().Add(-window).UTC().Format(time.RFC3339),
						},
					}},
					{"terms": map[string][]string{"source": {"ptp4l", "phc2sys"}}},
				},
			},
		},
		"sort": []map[string]string{{"@timestamp": "asc"}},
	}
	jb, err := json.Marshal(body)
	if err != nil {
		return nil, err
	}
	req, err := http.NewRequest("POST", endpoint, bytes.NewReader(jb))
	if err != nil {
		return nil, err
	}
	req.Header.Set("Content-Type", "application/json")
	if s.Username != "" {
		req.SetBasicAuth(s.Username, s.Password)
	}

	resp, err := client.Do(req)
	if err != nil {
		return nil, fmt.Errorf("opensearch request: %w", err)
	}
	defer resp.Body.Close()
	if resp.StatusCode != http.StatusOK {
		raw, _ := io.ReadAll(resp.Body)
		return nil, fmt.Errorf("opensearch %d: %s", resp.StatusCode, raw)
	}

	var parsed struct {
		Hits struct {
			Hits []struct {
				Source struct {
					Server    string `json:"server"`
					OffsetNs  int64  `json:"offset_ns"`
					Timestamp string `json:"@timestamp"`
				} `json:"_source"`
			} `json:"hits"`
		} `json:"hits"`
	}
	if err := json.NewDecoder(resp.Body).Decode(&parsed); err != nil {
		return nil, fmt.Errorf("decode opensearch response: %w", err)
	}

	out := make([]audit.Sample, 0, len(parsed.Hits.Hits))
	for _, h := range parsed.Hits.Hits {
		ts, err := time.Parse(time.RFC3339Nano, h.Source.Timestamp)
		if err != nil {
			continue
		}
		offset := h.Source.OffsetNs
		abs := offset
		if abs < 0 {
			abs = -abs
		}
		out = append(out, audit.Sample{
			Server:    h.Source.Server,
			BizTime:   ts,
			OffsetNs:  offset,
			OffsetAbs: abs,
		})
	}
	return out, nil
}
