/*
 * Azure Kubernetes Service cluster module — Phase 2 Skeleton.
 * Wird in Phase 14 (Multi-Region) je Region instanziiert.
 */
terraform {
  required_version = ">= 1.9"
  required_providers {
    azurerm = {
      source  = "hashicorp/azurerm"
      version = "~> 4.0"
    }
  }
}

variable "name" {
  description = "AKS cluster name (e.g., tms-prod-shadow-zh)"
  type        = string
}

variable "location" {
  description = "Azure region (e.g., switzerlandnorth, uksouth, eastus, japaneast)"
  type        = string
}

variable "node_count" {
  description = "Initial node count for the default node pool"
  type        = number
  default     = 3
}

variable "vm_size" {
  description = "VM size for default node pool"
  type        = string
  default     = "Standard_D8s_v5"
}

resource "azurerm_resource_group" "this" {
  name     = "rg-${var.name}"
  location = var.location
}

resource "azurerm_kubernetes_cluster" "this" {
  name                = var.name
  location            = azurerm_resource_group.this.location
  resource_group_name = azurerm_resource_group.this.name
  dns_prefix          = var.name
  kubernetes_version  = "1.30.4"

  default_node_pool {
    name       = "default"
    node_count = var.node_count
    vm_size    = var.vm_size
  }

  identity {
    type = "SystemAssigned"
  }

  tags = {
    project     = "swisstms"
    environment = var.name
    region      = var.location
  }
}

output "cluster_name" {
  value = azurerm_kubernetes_cluster.this.name
}

output "kube_config" {
  value     = azurerm_kubernetes_cluster.this.kube_config_raw
  sensitive = true
}
