# PTP Grandmaster Simulator

Software-PTP-Grandmaster für lokale Dev/CI. Wird in Phase 11 (US9) per
`docker run linuxptp/ptp4l` aufgesetzt; bis dahin reicht `chrony` als
NTP-Quelle (Service in `compose.dev.yaml`).

Production verwendet **Meinberg LANTIME M3000** mit Boundary-Clock pro
Cabinet — siehe `infra/ansible/playbooks/ptp-grandmaster.yml` (Phase 11).
