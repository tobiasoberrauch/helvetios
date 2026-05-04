# T334 — OpenBao PKI configuration.
#
# OpenBao (the OSS Vault fork) is our internal PKI. We mount three PKI engines:
#   - swisstms-internal-ca/         long-lived root, offline-rotated annually
#   - swisstms-platform-svc/        intermediate that signs every workload cert (Linkerd, mTLS)
#   - swisstms-eurex-clearing/      intermediate scoped to Eurex Clearing AMQP TLS certs
#
# cert-manager Issuers reference these engines so workload certs can be auto-renewed without
# any human in the loop. Constitution Principle VI — every issuance event lands in the audit
# chain via the OpenBao audit-log → Kafka relay.

terraform {
  required_version = ">= 1.8.0"
  required_providers {
    openbao = {
      source  = "openbao/openbao"
      version = "~> 0.10"
    }
  }
}

variable "trust_domain" {
  type    = string
  default = "swisstms.ch"
}

variable "ca_ttl_hours" {
  type    = number
  default = 87600 # 10 years
}

variable "intermediate_ttl_hours" {
  type    = number
  default = 8760 # 1 year — auto-rotated 30d before expiry
}

resource "openbao_mount" "internal_ca" {
  path        = "swisstms-internal-ca"
  type        = "pki"
  description = "Swiss-TMS internal root CA — offline-rotated annually"
  default_lease_ttl_seconds = var.ca_ttl_hours * 3600
  max_lease_ttl_seconds     = var.ca_ttl_hours * 3600
}

resource "openbao_pki_secret_backend_root_cert" "internal_ca" {
  backend     = openbao_mount.internal_ca.path
  type        = "internal"
  common_name = "Swiss-TMS Internal CA"
  organization = "Swiss-TMS"
  country     = "CH"
  ttl         = var.ca_ttl_hours * 3600
  key_type    = "rsa"
  key_bits    = 4096
}

resource "openbao_mount" "platform_svc" {
  path        = "swisstms-platform-svc"
  type        = "pki"
  description = "Swiss-TMS workload-identity intermediate (signed by internal-ca)"
  default_lease_ttl_seconds = var.intermediate_ttl_hours * 3600
  max_lease_ttl_seconds     = var.intermediate_ttl_hours * 3600
}

resource "openbao_pki_secret_backend_role" "platform_svc_default" {
  backend         = openbao_mount.platform_svc.path
  name            = "swisstms-workload"
  allowed_domains = [var.trust_domain]
  allow_subdomains = true
  allow_bare_domains = false
  ttl             = "168h"  # 7 days, refreshed by cert-manager
  max_ttl         = "720h"  # 30 days hard cap
  key_type        = "rsa"
  key_bits        = 2048
  key_usage       = ["DigitalSignature", "KeyEncipherment"]
  ext_key_usage   = ["ServerAuth", "ClientAuth"]
}

resource "openbao_mount" "eurex_clearing" {
  path        = "swisstms-eurex-clearing"
  type        = "pki"
  description = "Eurex Clearing AMQP client certs (FIXED FROM RFC-cycle: Sep rotation)"
  default_lease_ttl_seconds = 8760 * 3600
  max_lease_ttl_seconds     = 8760 * 3600
}

resource "openbao_pki_secret_backend_role" "eurex_amqp_client" {
  backend         = openbao_mount.eurex_clearing.path
  name            = "eurex-amqp-client"
  allowed_domains = ["eurex.${var.trust_domain}"]
  allow_subdomains = true
  ttl             = "8760h"  # 1 year — Eurex contract default
  max_ttl         = "8760h"
  key_type        = "rsa"
  key_bits        = 4096
  key_usage       = ["DigitalSignature", "KeyEncipherment"]
  ext_key_usage   = ["ClientAuth"]
}

output "platform_svc_mount_path" {
  value = openbao_mount.platform_svc.path
}

output "eurex_clearing_mount_path" {
  value = openbao_mount.eurex_clearing.path
}
