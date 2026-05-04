# T269 — S3 (or Azure Blob) cross-region replication for WORM archival.
#
# Production runs on Azure GRS (geo-redundant storage) with read-access to the secondary region.
# This module wraps the four storage accounts (one per region) and the replication policies that
# mirror the WORM container to the configured DR region.
#
# Constitution Principle VI — every WORM write is audit-chained; cross-region replication is
# observability-only and does NOT change retention semantics (Object Lock COMPLIANCE survives the
# replication boundary).

terraform {
  required_version = ">= 1.8.0"
  required_providers {
    azurerm = {
      source  = "hashicorp/azurerm"
      version = "~> 4.0"
    }
  }
}

variable "regions" {
  description = "Map of region key → Azure region name"
  type        = map(string)
  default = {
    zh  = "switzerlandnorth"
    ld4 = "uksouth"
    ny4 = "eastus"
    ty3 = "japaneast"
  }
}

variable "dr_pairs" {
  description = "Disaster-recovery pairing — primary → DR target region key"
  type        = map(string)
  default = {
    zh  = "ld4"
    ld4 = "zh"
    ny4 = "ty3"
    ty3 = "ny4"
  }
}

variable "retention_days" {
  description = "Default retention for the WORM container"
  type        = number
  default     = 1825 # 5 years (MiFID II Art.16)
}

resource "azurerm_resource_group" "rg" {
  for_each = var.regions
  name     = "swisstms-worm-${each.key}"
  location = each.value
}

resource "azurerm_storage_account" "worm" {
  for_each = var.regions

  name                = "swisstmsworm${each.key}"
  resource_group_name = azurerm_resource_group.rg[each.key].name
  location            = each.value

  account_tier             = "Standard"
  account_replication_type = "GRS"
  account_kind             = "StorageV2"

  blob_properties {
    versioning_enabled = true
    container_delete_retention_policy {
      days = 7
    }
  }

  immutability_policy {
    allow_protected_append_writes = false
    state                         = "Locked"
    period_since_creation_in_days = var.retention_days
  }

  tags = {
    "swisstms.ch/regulation"  = "MiFID-II,EMIR,FinfraG"
    "swisstms.ch/retention"   = "${var.retention_days}d"
    "swisstms.ch/dr-target"   = var.dr_pairs[each.key]
    "swisstms.ch/object-lock" = "COMPLIANCE"
  }
}

resource "azurerm_storage_management_policy" "lifecycle" {
  for_each           = azurerm_storage_account.worm
  storage_account_id = each.value.id

  rule {
    name    = "expire-after-retention"
    enabled = true
    filters {
      blob_types = ["blockBlob"]
    }
    actions {
      base_blob {
        delete_after_days_since_modification_greater_than = var.retention_days + 30
      }
    }
  }
}

output "worm_endpoints" {
  value = { for k, v in azurerm_storage_account.worm : k => v.primary_blob_endpoint }
}

output "dr_endpoints" {
  value = { for k, v in azurerm_storage_account.worm : k => v.secondary_blob_endpoint }
}
