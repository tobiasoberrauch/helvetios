# 5-Minute Demo Video (T231)

**Status:** Placeholder — pending recording.

Once Phase 14 brings `tilt up` end-to-end against the venue mocks, the recording walks through:

| Time   | Section | Artefact |
|--------|---------|----------|
| 0:00–0:30 | Opening — what the platform is | `README.md` § "Industry-Expert-Empfehlung" |
| 0:30–1:30 | C4 container view | `docs/architecture/containers.md` |
| 1:30–2:30 | Submit an order, see it round-trip | `task oms:curl:order` + Tilt UI |
| 2:30–3:30 | Drop-copy reconciliation | `apps/reconciler-service/` + Grafana panel |
| 3:30–4:30 | Audit chain — tamper detection | `task ptp-audit` + `cosign verify-blob` |
| 4:30–5:00 | Constitution gates in CI | `.github/workflows/lint.yml` + ArchUnit summary |

The recording will live at `docs/interview/demo-video.mp4` (Git LFS) and be linked from
[`README.md`](../../README.md).
