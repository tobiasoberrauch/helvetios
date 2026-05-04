/*
 * Aurora Global Database for cross-region Postgres replication.
 * Phase 2 — Skeleton. Phase 14 instantiates per-region.
 *
 * Primary cluster: Zurich. Secondary read clusters: LD4, NY4, TY3.
 */
terraform {
  required_version = ">= 1.9"
  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 5.50"
    }
  }
}

variable "global_cluster_id" {
  description = "Global identifier — e.g., 'swisstms-postgres-global'"
  type        = string
}

variable "primary_region" {
  description = "AWS region for the primary writer (e.g., eu-central-2 = Zurich)"
  type        = string
  default     = "eu-central-2"
}

resource "aws_rds_global_cluster" "this" {
  global_cluster_identifier = var.global_cluster_id
  engine                    = "aurora-postgresql"
  engine_version            = "16.4"
  database_name             = "swisstms"
  storage_encrypted         = true
}

# Per-region cluster instances werden in den environments/<env>/main.tf
# eingebunden, weil sie auf Provider-Aliase pro Region angewiesen sind.

output "global_cluster_arn" {
  value = aws_rds_global_cluster.this.arn
}
