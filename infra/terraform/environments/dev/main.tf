terraform {
  required_version = ">= 1.9"
  required_providers { azurerm = { source = "hashicorp/azurerm", version = "~> 4.0" } }
  backend "azurerm" {
    resource_group_name  = "tfstate-dev"
    storage_account_name = "tfstatedev"
    container_name       = "tfstate"
    key                  = "swisstms.tfstate"
  }
}

provider "azurerm" {
  features {}
}

module "aks" {
  source     = "../../modules/aks-cluster"
  name       = "tms-dev"
  location   = "switzerlandnorth"
  node_count = 3
  vm_size    = "Standard_D8s_v5"
}

# Per-region prod-shadow tags additional metadata for ops dashboards.
locals {
  region_pop = ""
}
